import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

/**
 * ========================================
 * 선착순 쿠폰 발급 스파이크 테스트
 * ========================================
 * 
 * 시나리오: 한정 수량 쿠폰을 다수의 사용자가 동시에 발급 요청
 * 
 * 테스트 목적:
 * 1. 동시성 제어 검증 - 정확히 N개만 발급되는가?
 * 2. 중복 발급 방지 - 동일 사용자가 2번 받지 않는가?
 * 3. 응답 시간 - 스파이크 상황에서 응답 지연은?
 * 4. 시스템 안정성 - 500 에러 없이 처리되는가?
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ========================================
// 테스트 파라미터
// ========================================
const COUPON_QUANTITY = 100;    // 쿠폰 수량 (한정)
const TOTAL_USERS = 1000;       // 총 요청 사용자 수
const SPIKE_DURATION = '10s';   // 스파이크 지속 시간
const RAMP_UP = '1s';           // 스파이크 도달 시간

// ========================================
// 커스텀 메트릭
// ========================================
const issuedSuccess = new Counter('issued_success');        // 발급 성공 수
const issuedSoldOut = new Counter('issued_sold_out');       // 재고 소진
const issuedDuplicate = new Counter('issued_duplicate');    // 중복 발급 시도
const issuedError = new Counter('issued_error');            // 시스템 에러

const successRate = new Rate('success_rate');               // 성공률
const errorRate = new Rate('error_rate');                   // 에러율
const responseTime = new Trend('response_time', true);      // 응답 시간

// ========================================
// 테스트 옵션
// ========================================
export const options = {
    scenarios: {
        spike_test: {
            executor: 'ramping-arrival-rate',
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: 200,
            maxVUs: TOTAL_USERS,
            stages: [
                { duration: '3s', target: 0 },              // 준비
                { duration: RAMP_UP, target: TOTAL_USERS }, // 스파이크 (1초 만에 최대 도달)
                { duration: SPIKE_DURATION, target: TOTAL_USERS }, // 유지
                { duration: '3s', target: 0 },              // 종료
            ],
        },
    },
    thresholds: {
        // 성능 기준
        'http_req_duration': ['p(95)<2000', 'p(99)<3000'],  // 95%가 2초 이내
        'response_time': ['p(95)<2000'],
        
        // 안정성 기준
        'error_rate': ['rate<0.01'],                        // 시스템 에러 1% 미만
        
        // 비즈니스 기준 (정확히 COUPON_QUANTITY 개만 발급)
        'issued_success': [`count>=${COUPON_QUANTITY * 0.95}`, `count<=${COUPON_QUANTITY}`],
    },
};

// ========================================
// Setup: 쿠폰 생성
// ========================================
export function setup() {
    console.log('\n');
    console.log('╔════════════════════════════════════════════════════════════╗');
    console.log('║          선착순 쿠폰 발급 스파이크 테스트                     ║');
    console.log('╠════════════════════════════════════════════════════════════╣');
    console.log(`║  쿠폰 수량     : ${COUPON_QUANTITY.toString().padStart(6)} 개                               ║`);
    console.log(`║  동시 사용자   : ${TOTAL_USERS.toString().padStart(6)} 명                               ║`);
    console.log(`║  예상 성공률   : ${((COUPON_QUANTITY / TOTAL_USERS) * 100).toFixed(1).padStart(6)} %                               ║`);
    console.log(`║  스파이크 시간 : ${SPIKE_DURATION.padStart(6)}                                  ║`);
    console.log('╚════════════════════════════════════════════════════════════╝');
    console.log('\n');

    // 쿠폰 생성 요청
    const createRes = http.post(
        `${BASE_URL}/coupons`,
        JSON.stringify({ quantity: COUPON_QUANTITY }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    const createCheck = check(createRes, {
        '[Setup] 쿠폰 생성 성공 (200)': (r) => r.status === 200,
        '[Setup] 응답 형식 유효': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.result === 'SUCCESS' && body.data && body.data.id;
            } catch {
                return false;
            }
        },
    });

    if (!createCheck) {
        console.error('❌ Setup 실패: 쿠폰 생성 불가');
        console.error(`   Status: ${createRes.status}`);
        console.error(`   Body: ${createRes.body}`);
        throw new Error('Setup failed');
    }

    const couponId = JSON.parse(createRes.body).data.id;
    console.log(`✅ 쿠폰 생성 완료 (ID: ${couponId})`);
    console.log('\n⏳ 스파이크 테스트 시작...\n');

    return {
        couponId: couponId,
        startTime: Date.now(),
    };
}

// ========================================
// 메인 테스트: 쿠폰 발급 요청
// ========================================
export default function (data) {
    // 고유 사용자 ID 생성 (VU ID + iteration 조합)
    const userId = __VU * 100000 + __ITER;
    
    const startTime = Date.now();
    
    const res = http.post(
        `${BASE_URL}/coupons/${data.couponId}/issues`,
        JSON.stringify({ userId: userId }),
        {
            headers: { 'Content-Type': 'application/json' },
            tags: { name: 'IssueCoupon' },
        }
    );

    const duration = Date.now() - startTime;
    responseTime.add(duration);

    // 응답 분류
    if (res.status === 200) {
        // 발급 성공
        issuedSuccess.add(1);
        successRate.add(true);
        errorRate.add(false);
    } else if (res.status === 410) {
        // 재고 소진 (정상적인 실패)
        issuedSoldOut.add(1);
        successRate.add(false);
        errorRate.add(false);
    } else if (res.status === 409) {
        // 중복 발급 (정상적인 실패)
        issuedDuplicate.add(1);
        successRate.add(false);
        errorRate.add(false);
    } else {
        // 시스템 에러 (비정상)
        issuedError.add(1);
        successRate.add(false);
        errorRate.add(true);
        console.error(`❌ [ERROR] userId=${userId}, status=${res.status}, body=${res.body}`);
    }

    // 응답 검증
    check(res, {
        '응답 상태 유효 (2xx/4xx)': (r) => r.status >= 200 && r.status < 500,
        '응답 시간 < 2초': (r) => r.timings.duration < 2000,
        '응답 형식 유효 (JSON)': (r) => {
            try {
                JSON.parse(r.body);
                return true;
            } catch {
                return false;
            }
        },
    });
}

// ========================================
// Teardown: 결과 검증
// ========================================
export function teardown(data) {
    sleep(2); // 마지막 요청 완료 대기

    const totalTime = ((Date.now() - data.startTime) / 1000).toFixed(2);

    // 최종 쿠폰 상태 조회
    const res = http.get(`${BASE_URL}/coupons`);
    
    console.log('\n');
    console.log('╔════════════════════════════════════════════════════════════╗');
    console.log('║                    테스트 결과 리포트                        ║');
    console.log('╠════════════════════════════════════════════════════════════╣');
    console.log(`║  총 소요 시간  : ${totalTime.padStart(6)} 초                               ║`);
    
    if (res.status === 200) {
        try {
            const body = JSON.parse(res.body);
            const coupons = body.data.coupons || [];
            const coupon = coupons.find(c => c.id === data.couponId);
            
            if (coupon) {
                const issued = coupon.issuedQuantity;
                const total = coupon.totalQuantity;
                const remaining = total - issued;
                
                console.log('╠════════════════════════════════════════════════════════════╣');
                console.log(`║  쿠폰 ID       : ${data.couponId.toString().padStart(6)}                                  ║`);
                console.log(`║  총 수량       : ${total.toString().padStart(6)} 개                               ║`);
                console.log(`║  발급 수량     : ${issued.toString().padStart(6)} 개                               ║`);
                console.log(`║  잔여 수량     : ${remaining.toString().padStart(6)} 개                               ║`);
                console.log('╠════════════════════════════════════════════════════════════╣');
                
                // 결과 판정
                if (issued === COUPON_QUANTITY) {
                    console.log('║  ✅ 결과: 성공 - 정확히 발급됨                               ║');
                } else if (issued > COUPON_QUANTITY) {
                    console.log('║  ❌ 결과: 실패 - 초과 발급 (동시성 버그)                     ║');
                } else if (issued < COUPON_QUANTITY * 0.95) {
                    console.log('║  ⚠️  결과: 경고 - 미달 발급 (95% 미만)                       ║');
                } else {
                    console.log('║  ✅ 결과: 성공 - 허용 범위 내 발급                          ║');
                }
            } else {
                console.log('║  ⚠️  쿠폰 정보를 찾을 수 없음                                 ║');
            }
        } catch (e) {
            console.log('║  ❌ 응답 파싱 실패                                           ║');
        }
    } else {
        console.log('║  ❌ 쿠폰 조회 실패                                            ║');
    }
    
    console.log('╚════════════════════════════════════════════════════════════╝');
    console.log('\n');
}

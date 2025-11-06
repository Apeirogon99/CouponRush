import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = 'http://localhost:8080';
const COUPON_ID = 1;
const COUPON_QUANTITY = 100;
const TOTAL_USERS = 2000; // 쿠폰 수량보다 많게 설정

const successRate = new Rate('coupon_issue_success');

export const options = {
    stages: [
        { duration: '5s', target: 0 },      // 대기
        { duration: '3s', target: 1000 },    // 3초 안에 1000명 동시 접속
        { duration: '10s', target: 1000 },   // 10초간 유지
        { duration: '5s', target: 0 },      // 종료
    ],
    thresholds: {
        http_req_duration: ['p(95)<3000'], // 95%의 요청이 3초 이내
        http_req_failed: ['rate<0.5'],     // 50% 이상 성공
    },
};

// 테스트 시작 전 쿠폰 초기화
export function setup() {
    console.log(`\n=== Setup: Initializing Coupon ${COUPON_ID} with quantity ${COUPON_QUANTITY} ===`);
    
    const initRes = http.get(`${BASE_URL}/${COUPON_ID}/init?quantity=${COUPON_QUANTITY}`);
    
    check(initRes, {
        'coupon init status is 200': (r) => r.status === 200,
    });
    
    if (initRes.status !== 200) {
        console.error('Failed to initialize coupon');
    } else {
        console.log('Coupon initialized successfully');
    }
    
    return { couponId: COUPON_ID };
}

export default function (data) {
    const userId = Math.floor(Math.random() * TOTAL_USERS) + 1;

    const res = http.post(
        `${BASE_URL}/${data.couponId}/issue?userId=${userId}`,
        null,
        {
            headers: { 'Content-Type': 'application/json' },
            tags: { name: 'IssueCoupon' },
        }
    );

    const isSuccess = check(res, {
        'status is 200': (r) => r.status === 200,
    });

    // 컨트롤러는 String을 반환하므로 status 200이면 성공으로 간주
    if (res.status === 200) {
        successRate.add(true);
        // console.log(`[SUCCESS] userId ${userId}: ${res.body}`);
    } else {
        successRate.add(false);
        // console.log(`[FAIL] userId ${userId}: status=${res.status}`);
    }

    sleep(0.1);
}

// 테스트 후 쿠폰 재고 확인
export function teardown(data) {
    console.log('\n=== Teardown: Checking Final Coupon Status ===');
    
    const res = http.get(`${BASE_URL}/${data.couponId}/count`);
    
    if (res.status === 200) {
        const coupon = JSON.parse(res.body);
        console.log(`\n=== Final Coupon Status ===`);
        console.log(`Coupon ID: ${coupon.id}`);
        console.log(`Total Quantity: ${coupon.totalQuantity}`);
        console.log(`Issued Quantity: ${coupon.issuedQuantity}`);
        console.log(`Remaining: ${coupon.totalQuantity - coupon.issuedQuantity}`);
    } else {
        console.error('Failed to get coupon status');
    }
}
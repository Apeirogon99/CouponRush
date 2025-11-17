import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

const BASE_URL = 'http://localhost:8080';
const COUPON_ID = 1;
const COUPON_QUANTITY = 2000;
const TOTAL_USERS = 4000;

const successRate = new Rate('coupon_issue_success');
const soldOutRate = new Rate('coupon_sold_out');
const duplicateRate = new Rate('coupon_duplicate_issue');
const systemErrorRate = new Rate('system_error');
const successfulIssues = new Counter('successful_issues_count');
const responseTime = new Trend('issue_response_time');

export const options = {
    scenarios: {
        spike_test: {
            executor: 'ramping-arrival-rate',
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: COUPON_QUANTITY,
            maxVUs: TOTAL_USERS,
            stages: [
                { duration: '5s', target: 0 },
                { duration: '1s', target: TOTAL_USERS },
                { duration: '15s', target: TOTAL_USERS },
                { duration: '5s', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<3000', 'p(99)<5000'],
        'system_error': ['rate<0.01'],
        'successful_issues_count': [`count==${COUPON_QUANTITY}`],
    },
};

export function setup() {
    console.log(`\n=== Setup: Initializing Coupon ${COUPON_ID} ===`);
    console.log(`Quantity: ${COUPON_QUANTITY}`);
    console.log(`Total Users: ${TOTAL_USERS}`);
    console.log(`Expected Success Rate: ${(COUPON_QUANTITY / TOTAL_USERS * 100).toFixed(2)}%`);

    const initRes = http.post(`${BASE_URL}/${COUPON_ID}/init?quantity=${COUPON_QUANTITY}`);

    check(initRes, {
        'coupon init status is 200': (r) => r.status === 200,
    });

    if (initRes.status !== 200) {
        console.error('Failed to initialize coupon');
        throw new Error('Setup failed');
    }

    console.log('✅ Coupon initialized successfully\n');

    return { couponId: COUPON_ID, startTime: Date.now() };
}

export default function (data) {
    const userId = Math.floor(Math.random() * TOTAL_USERS) + 1;
    const startTime = Date.now();

    const res = http.post(
        `${BASE_URL}/${data.couponId}/issue?userId=${userId}`,
        null,
        {
            headers: { 'Content-Type': 'application/json' },
            tags: { name: 'IssueCoupon' },
        }
    );

    const duration = Date.now() - startTime;
    responseTime.add(duration);

    // 상세한 결과 분류
    if (res.status === 200) {
        successRate.add(true);
        successfulIssues.add(1);
    } else if (res.status === 410) {  // 재고 소진
        successRate.add(false);
        soldOutRate.add(true);
    } else if (res.status === 409) {  // 중복 발급
        successRate.add(false);
        duplicateRate.add(true);
    } else {  // 시스템 에러
        successRate.add(false);
        systemErrorRate.add(true);
        console.error(`[SYSTEM ERROR] userId ${userId}: status=${res.status}`);
    }

    check(res, {
        'status is 2xx or 4xx': (r) => r.status >= 200 && r.status < 500,
        'response time < 3s': (r) => r.timings.duration < 3000,
    });

    sleep(0.05);  // ✅ 더 짧게 (더 많은 요청)
}

export function teardown(data) {
    const totalTime = ((Date.now() - data.startTime) / 1000).toFixed(2);

    console.log('\n========================================');
    console.log('=== LOAD TEST COMPLETED ===');
    console.log('========================================');
    console.log(`Total Test Duration: ${totalTime}s`);

    sleep(2);  // 마지막 요청 완료 대기

    const res = http.get(`${BASE_URL}/${data.couponId}/count`);

    if (res.status === 200) {
        const coupon = JSON.parse(res.body);
        const remaining = coupon.totalQuantity - coupon.issuedQuantity;

        console.log('\n=== FINAL COUPON STATUS ===');
        console.log(`Coupon ID: ${coupon.id}`);
        console.log(`Total Quantity: ${coupon.totalQuantity}`);
        console.log(`Issued Quantity: ${coupon.issuedQuantity}`);
        console.log(`Remaining: ${remaining}`);

        // ✅ 검증
        if (coupon.issuedQuantity === COUPON_QUANTITY) {
            console.log('✅ SUCCESS: All coupons issued correctly!');
        } else if (coupon.issuedQuantity > COUPON_QUANTITY) {
            console.log('❌ FAILURE: Over-issued! Race condition detected!');
        } else {
            console.log('⚠️  WARNING: Under-issued. Check logs.');
        }

        console.log('========================================\n');
    } else {
        console.error('❌ Failed to get coupon status');
    }
}
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = 'http://localhost:8080';
const COUPON_ID = 1;
const TOTAL_USERS = 2000; // 쿠폰 수량보다 많게 설정

const successRate = new Rate('coupon_issue_success'); // success : false를 잡기 위해

export const options = {
    stages: [
        { duration: '5s', target: 0 },      // 대기
        { duration: '3s', target: 1000 },    // 3초 안에 500명 동시 접속
        { duration: '10s', target: 1000 },   // 10초간 유지
        { duration: '5s', target: 0 },      // 종료
    ],
    thresholds: {
        http_req_duration: ['p(95)<3000'], // 95%의 요청이 3초 이내
        http_req_failed: ['rate<0.5'],     // 50% 이상 성공
    },
};

export default function () {
    const userId = Math.floor(Math.random() * TOTAL_USERS) + 1;

    const res = http.post(
        `${BASE_URL}/api/coupons/${COUPON_ID}/issue?userId=${userId}`,
        null,
        {
            headers: { 'Content-Type': 'application/json' },
            tags: { name: 'IssueCoupon' },
        }
    );

    const isSuccess = check(res, {
        'status is 200': (r) => r.status === 200,
        'response has success field': (r) => {
            try {
                return JSON.parse(r.body).hasOwnProperty('success');
            } catch {
                return false;
            }
        },
    });

    if (res.status === 200) {
        const body = JSON.parse(res.body);
        successRate.add(body.success);

        if (!body.success) {
            // console.log(`[FAIL] userId ${userId}: ${body.message}`);
        }
    }

    sleep(0.1);
}

// 테스트 후 쿠폰 재고 확인
export function handleSummary(data) {
    const res = http.get(`${BASE_URL}/api/coupons`);
    const coupons = JSON.parse(res.body);
    const targetCoupon = coupons.find(c => c.id === COUPON_ID);

    console.log(`\n=== Coupon Status ===`);
    console.log(`Total: ${targetCoupon.totalQuantity}`);
    console.log(`Issued: ${targetCoupon.issuedQuantity}`);
    console.log(`Success Rate: ${(data.metrics.coupon_issue_success.values.rate * 100).toFixed(2)}%`);

    return {
        'stdout': JSON.stringify(data, null, 2),
    };
}
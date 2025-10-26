package im.bigs.pg.domain.pg

/**
 * PG(Payment Gateway) 타입.
 *
 * 나노바나나 페이먼츠가 연동하는 외부 결제 게이트웨이 종류를 정의합니다.
 *
 * @property MOCK 목업 PG - 테스트용 가상 PG (항상 성공)
 * @property TEST_PG TestPG API - 실제 연동 테스트용 PG
 * @property DUMMY_PAY DummyPay - 추가 PG 예시 (Failover 테스트용)
 */
enum class PgType {
    MOCK,
    TEST_PG,
    DUMMY_PAY
}

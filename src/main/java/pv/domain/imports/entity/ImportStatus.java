package pv.domain.imports.entity;

public enum ImportStatus {
    PROCESSING, // ZIP 처리 중
    READY,      // 처리 완료, 미리보기 확인 가능 (확정 대기)
    CONFIRMED,  // 확정되어 저장 완료
    FAILED      // 처리 실패
}
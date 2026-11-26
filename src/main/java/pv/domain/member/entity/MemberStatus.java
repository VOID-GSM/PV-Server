package pv.domain.member.entity;

// LEFT: 부장이 탈퇴 처리한 회원 (기록은 남기고 로그인·재입장 불가)
public enum MemberStatus { PENDING, JOINED, LEFT }
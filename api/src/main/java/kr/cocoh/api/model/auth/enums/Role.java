package kr.cocoh.api.model.auth.enums;

public enum Role {
    USER, ADMIN, SUPERADMIN;
    
    // Spring Security에서 사용하는 권한 문자열 형식으로 변환
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
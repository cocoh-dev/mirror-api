-- Users 테이블 생성
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    provider ENUM('local', 'google', 'kakao', 'naver') NOT NULL DEFAULT 'local',
    provider_id VARCHAR(255),
    role ENUM('user', 'admin', 'superadmin') NOT NULL DEFAULT 'user',
    profile_image VARCHAR(255),
    refresh_token TEXT,
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_email (email)
);

-- Salons 테이블 생성
CREATE TABLE salons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    business_hours VARCHAR(100) NOT NULL,
    business_number VARCHAR(10) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    description TEXT,
    status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_business_number (business_number),
    FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Locations 테이블 생성
CREATE TABLE locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    salon_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE
);

-- Displays 테이블 생성
CREATE TABLE displays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    access_token VARCHAR(255) NOT NULL,
    status ENUM('active', 'inactive', 'maintenance') NOT NULL DEFAULT 'active',
    last_ping TIMESTAMP NULL,
    settings JSON,
    salon_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_device_id (device_id),
    UNIQUE KEY unique_access_token (access_token),
    FOREIGN KEY (salon_id) REFERENCES salons (id) ON DELETE CASCADE
);

-- User Activity 테이블 생성
CREATE TABLE user_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    details JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);
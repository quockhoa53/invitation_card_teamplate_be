-- ==============================================================================
-- SCRIPT BỔ SUNG CỘT VÀ BẢNG MỚI CHO CƠ SỞ DỮ LIỆU POSTGRESQL
-- Hệ thống: Invitation Card Template
-- Hỗ trợ: Ví 2 tầng, Bóc tách khuyến mãi, Xử lý chuyển thiếu, Duyệt rút tiền, 
--         Mã giảm giá và Quyền sở hữu mẫu thiệp.
-- ==============================================================================

-- 1. BỔ SUNG CỘT CHO BẢNG USERS (Tách bạch Ví tiền nạp thật & Tiền thưởng)
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS real_balance BIGINT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS bonus_balance BIGINT NOT NULL DEFAULT 0;

-- Đồng bộ dữ liệu cũ: chuyển credits_balance hiện có sang real_balance
UPDATE users 
SET real_balance = credits_balance 
WHERE real_balance = 0 AND credits_balance > 0;


-- 2. BỔ SUNG CỘT CHO BẢNG TRANSACTIONS (Ghi nhận tiền thưởng, số tiền thiếu, loại GD)
ALTER TABLE transactions 
ADD COLUMN IF NOT EXISTS bonus_amount BIGINT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS actual_amount BIGINT,
ADD COLUMN IF NOT EXISTS missing_amount BIGINT DEFAULT 0,
ADD COLUMN IF NOT EXISTS type VARCHAR(30) NOT NULL DEFAULT 'DEPOSIT';


-- 3. TẠO BẢNG WITHDRAWALS (Quản lý các yêu cầu rút tiền của khách)
CREATE TABLE IF NOT EXISTS withdrawals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount BIGINT NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_holder VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_note TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_withdrawals_user_id ON withdrawals(user_id);
CREATE INDEX IF NOT EXISTS idx_withdrawals_status ON withdrawals(status);


-- 4. TẠO BẢNG PROMOTIONS (Quản lý các mã khuyến mãi, voucher giảm giá)
CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL, -- 'PERCENTAGE' hoặc 'FIXED_AMOUNT'
    discount_value BIGINT NOT NULL,
    min_order_amount BIGINT DEFAULT 0,
    max_discount_amount BIGINT,
    max_usage INT DEFAULT 100,
    used_count INT DEFAULT 0,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions(code);
CREATE INDEX IF NOT EXISTS idx_promotions_is_active ON promotions(is_active);


-- 5. TẠO BẢNG USER_TEMPLATE_PURCHASES (Lưu trữ các mẫu thiệp người dùng đã mua/mở khóa)
CREATE TABLE IF NOT EXISTS user_template_purchases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id UUID NOT NULL REFERENCES templates(id) ON DELETE CASCADE,
    price_paid BIGINT NOT NULL,
    purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_template UNIQUE (user_id, template_id)
);

CREATE INDEX IF NOT EXISTS idx_utp_user_id ON user_template_purchases(user_id);
CREATE INDEX IF NOT EXISTS idx_utp_template_id ON user_template_purchases(template_id);

-- 6. CẬP NHẬT BẢNG USER_2FA (Hỗ trợ xác thực 2 bước gửi OTP qua Gmail)
ALTER TABLE user_2fa ADD COLUMN IF NOT EXISTS two_factor_type VARCHAR(20) DEFAULT 'EMAIL';
ALTER TABLE user_2fa ADD COLUMN IF NOT EXISTS email_otp_code VARCHAR(10);
ALTER TABLE user_2fa ADD COLUMN IF NOT EXISTS email_otp_expires_at TIMESTAMP;
ALTER TABLE user_2fa ALTER COLUMN encrypted_secret_key DROP NOT NULL;


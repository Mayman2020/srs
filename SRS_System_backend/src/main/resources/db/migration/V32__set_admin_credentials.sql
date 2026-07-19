UPDATE app_user SET username = 'admin', password_hash = '{bcrypt}$2b$10$49wu0oR2J3vEOrZkEGsLMuLFpKEt3nrQ9pnquwuvfZu2ceMvriOnq', is_active = TRUE, failed_login_count = 0, locked_until = NULL
WHERE username = 'admin' OR email = 'admin@local.invalid';

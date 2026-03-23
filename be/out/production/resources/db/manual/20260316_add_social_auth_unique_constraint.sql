ALTER TABLE social_auth
ADD CONSTRAINT uk_social_auth_provider_user UNIQUE (provider, provider_user_id);

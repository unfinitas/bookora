-- Change FK constraints from CASCADE to RESTRICT for data preservation

-- t_provider: user_id FK
ALTER TABLE t_provider
DROP CONSTRAINT fk_provider_user,
ADD CONSTRAINT fk_provider_user
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT;

-- t_service: provider_id FK
ALTER TABLE t_service
DROP CONSTRAINT fk_service_provider,
ADD CONSTRAINT fk_service_provider
    FOREIGN KEY (provider_id) REFERENCES t_provider(id) ON DELETE RESTRICT;

-- t_booking: customer_id, provider_id, service_id FKs
ALTER TABLE t_booking
DROP CONSTRAINT fk_booking_customer,
ADD CONSTRAINT fk_booking_customer
    FOREIGN KEY (customer_id) REFERENCES t_user(id) ON DELETE RESTRICT;

ALTER TABLE t_booking
DROP CONSTRAINT fk_booking_provider,
ADD CONSTRAINT fk_booking_provider
    FOREIGN KEY (provider_id) REFERENCES t_provider(id) ON DELETE RESTRICT;

ALTER TABLE t_booking
DROP CONSTRAINT fk_booking_service,
ADD CONSTRAINT fk_booking_service
    FOREIGN KEY (service_id) REFERENCES t_service(id) ON DELETE RESTRICT;

-- t_guest_access_token: booking_id FK
ALTER TABLE t_guest_access_token
DROP CONSTRAINT fk_guest_access_token_booking,
ADD CONSTRAINT fk_guest_access_token_booking
    FOREIGN KEY (booking_id) REFERENCES t_booking(id) ON DELETE RESTRICT;

-- reset-fretpayment-data.sql
-- WARNING: This truncates all payment data. Use only in dev/test environments.
-- Connects to fretpaymentdb on port 5434.
-- psql -h localhost -p 5434 -U postgres -d fretpaymentdb -f reset-fretpayment-data.sql

TRUNCATE TABLE fatourati_callback_log RESTART IDENTITY CASCADE;
TRUNCATE TABLE fatourati_token RESTART IDENTITY CASCADE;

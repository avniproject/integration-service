ALTER TABLE ONLY error_record_log
    ADD CONSTRAINT error_record_log_error_record FOREIGN KEY (error_record_id) REFERENCES error_record (id)
        ON DELETE CASCADE;
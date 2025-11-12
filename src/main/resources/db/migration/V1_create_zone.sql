```sql
CREATE TABLE IF NOT EXISTS zone (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  provider_id BIGINT,
  provider_type VARCHAR(50),
  name VARCHAR(255)
);

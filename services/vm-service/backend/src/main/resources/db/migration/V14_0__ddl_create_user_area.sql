SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE IF NOT EXISTS `user_area`
(
    id         BIGINT AUTO_INCREMENT NOT NULL
        PRIMARY KEY,
    user_id    VARCHAR(50)           NOT NULL COMMENT '用户ID',
    area_id    BIGINT                NOT NULL COMMENT '区域ID',
    area_name  VARCHAR(255)          NOT NULL COMMENT '区域名称',
    area_type  INT                   NOT NULL COMMENT '区域类型：1-国内，2-国外',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT UK_USER_AREA UNIQUE (user_id, area_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE utf8mb4_general_ci
    COMMENT '用户区域关系表';

CREATE INDEX IDX_USER_AREA_USER_ID ON user_area (user_id);
CREATE INDEX IDX_USER_AREA_AREA_ID ON user_area (area_id);
CREATE INDEX IDX_USER_AREA_AREA_TYPE ON user_area (area_type);

SET SESSION innodb_lock_wait_timeout = DEFAULT;

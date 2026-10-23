INSERT INTO tb_user(user_id,password,user_name,role_type) VALUES ('test_admin','test-only-unusable-password','Test Admin','ADMIN');
INSERT INTO tb_device(device_id,device_name,device_type,ip_address,device_status)
VALUES (75,'DCS test','L 대역 DCS','127.0.0.1','NORMAL'), (86,'Ka test','Ka 대역','127.0.0.1','NORMAL');
INSERT INTO tb_python_device_mapping VALUES ('DCS','44/56/01',75,TRUE),('KA','cam_test',86,TRUE);

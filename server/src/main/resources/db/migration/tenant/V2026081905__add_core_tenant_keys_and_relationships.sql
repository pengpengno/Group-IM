-- Core tenant baseline: primary keys, unique constraints and foreign-key relationships.
-- Constraint names preserve the reviewed legacy contract for drift comparison.

ALTER TABLE ONLY "approval_requests"
    ADD CONSTRAINT "approval_requests_execution_id_key" UNIQUE ("execution_id");

ALTER TABLE ONLY "approval_requests"
    ADD CONSTRAINT "approval_requests_pkey" PRIMARY KEY ("approval_id");

ALTER TABLE ONLY "automation_executions"
    ADD CONSTRAINT "automation_executions_idempotency_key_key" UNIQUE ("idempotency_key");

ALTER TABLE ONLY "automation_executions"
    ADD CONSTRAINT "automation_executions_pkey" PRIMARY KEY ("execution_id");

ALTER TABLE ONLY "automation_rules"
    ADD CONSTRAINT "automation_rules_pkey" PRIMARY KEY ("rule_id");

ALTER TABLE ONLY "conversation_bot_configs"
    ADD CONSTRAINT "conversation_bot_configs_conversation_id_key" UNIQUE ("conversation_id");

ALTER TABLE ONLY "conversation_bot_configs"
    ADD CONSTRAINT "conversation_bot_configs_pkey" PRIMARY KEY ("config_id");

ALTER TABLE ONLY "conversation_members"
    ADD CONSTRAINT "conversation_members_pkey" PRIMARY KEY ("id");

ALTER TABLE ONLY "conversations"
    ADD CONSTRAINT "conversations_pkey" PRIMARY KEY ("conversation_id");

ALTER TABLE ONLY "departments"
    ADD CONSTRAINT "departments_pkey" PRIMARY KEY ("department_id");

ALTER TABLE ONLY "file_resource"
    ADD CONSTRAINT "file_resource_pkey" PRIMARY KEY ("id");

ALTER TABLE ONLY "friendships"
    ADD CONSTRAINT "friendships_pkey" PRIMARY KEY ("id");

ALTER TABLE ONLY "media_file_resource"
    ADD CONSTRAINT "media_file_resource_file_id_key" UNIQUE ("file_id");

ALTER TABLE ONLY "media_file_resource"
    ADD CONSTRAINT "media_file_resource_pkey" PRIMARY KEY ("id");

ALTER TABLE ONLY "meeting_participants"
    ADD CONSTRAINT "meeting_participants_meeting_id_user_id_key" UNIQUE ("meeting_id", "user_id");

ALTER TABLE ONLY "meeting_participants"
    ADD CONSTRAINT "meeting_participants_pkey" PRIMARY KEY ("id");

ALTER TABLE ONLY "meetings"
    ADD CONSTRAINT "meetings_pkey" PRIMARY KEY ("meeting_id");

ALTER TABLE ONLY "meetings"
    ADD CONSTRAINT "meetings_room_id_key" UNIQUE ("room_id");

ALTER TABLE ONLY "messages"
    ADD CONSTRAINT "messages_pkey" PRIMARY KEY ("msg_id");

ALTER TABLE ONLY "status_updates"
    ADD CONSTRAINT "status_updates_pkey" PRIMARY KEY ("id");

ALTER TABLE ONLY "system_config_item"
    ADD CONSTRAINT "system_config_item_config_key_key" UNIQUE ("config_key");

ALTER TABLE ONLY "system_config_item"
    ADD CONSTRAINT "system_config_item_pkey" PRIMARY KEY ("id");

ALTER TABLE ONLY "upload_chunk_record"
    ADD CONSTRAINT "upload_chunk_record_pkey" PRIMARY KEY ("id");

ALTER TABLE ONLY "user_departments"
    ADD CONSTRAINT "user_departments_pkey" PRIMARY KEY ("id");

ALTER TABLE ONLY "user_privacy_settings"
    ADD CONSTRAINT "user_privacy_settings_pkey" PRIMARY KEY ("setting_id");

ALTER TABLE ONLY "user_privacy_settings"
    ADD CONSTRAINT "user_privacy_settings_user_id_key" UNIQUE ("user_id");

ALTER TABLE ONLY "automation_executions"
    ADD CONSTRAINT "fk205nr7k5fo7fsrsa4t2g0r7ll" FOREIGN KEY ("rule_id") REFERENCES "automation_rules"("rule_id");

ALTER TABLE ONLY "meeting_participants"
    ADD CONSTRAINT "fk47sc15ag6030wipkxxj0a1t8o" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "friendships"
    ADD CONSTRAINT "fk4mcscxflf13uk72aupf6uwbgn" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "automation_executions"
    ADD CONSTRAINT "fk4sk9olwm995w5euq9vftokpr" FOREIGN KEY ("conversation_id") REFERENCES "conversations"("conversation_id");

ALTER TABLE ONLY "conversations"
    ADD CONSTRAINT "fk5uxcbsjes7nd38wm1qtsfaw28" FOREIGN KEY ("created_by") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "approval_requests"
    ADD CONSTRAINT "fk5vk8ji1p25ymqxwej4qymydes" FOREIGN KEY ("execution_id") REFERENCES "automation_executions"("execution_id");

ALTER TABLE ONLY "automation_rules"
    ADD CONSTRAINT "fk7o205yyyyemqs5b2feitf7svq" FOREIGN KEY ("owner_user_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "meeting_participants"
    ADD CONSTRAINT "fk7uds89kvog9etbdnn653vsf6y" FOREIGN KEY ("meeting_id") REFERENCES "meetings"("meeting_id");

ALTER TABLE ONLY "meetings"
    ADD CONSTRAINT "fk91iiqwhms7obbc92so3wxqq6f" FOREIGN KEY ("created_by") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "conversation_bot_configs"
    ADD CONSTRAINT "fk953golnw0jb6h8liy8c4twvkq" FOREIGN KEY ("updated_by_user_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "conversation_bot_configs"
    ADD CONSTRAINT "fk9b5byt70ntxwxw8hac045vj2i" FOREIGN KEY ("conversation_id") REFERENCES "conversations"("conversation_id");

ALTER TABLE ONLY "user_privacy_settings"
    ADD CONSTRAINT "fkbjtqm30tcrdxb0dvtods9agbu" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "user_departments"
    ADD CONSTRAINT "fke1yilu7bslmau7ojj0n2pu812" FOREIGN KEY ("department_id") REFERENCES "departments"("department_id");

ALTER TABLE ONLY "user_departments"
    ADD CONSTRAINT "fkeklynfw1mm4x2289n61pj0ojn" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "media_file_resource"
    ADD CONSTRAINT "fkfepu7hvitga29anr95lgsevlo" FOREIGN KEY ("file_id") REFERENCES "file_resource"("id");

ALTER TABLE ONLY "status_updates"
    ADD CONSTRAINT "fkfq5kjypxeeh4avna36muy9ymg" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "approval_requests"
    ADD CONSTRAINT "fkl1hpw1j6p662psu8hpc19m1oy" FOREIGN KEY ("requested_by_user_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "meetings"
    ADD CONSTRAINT "fkldb23d3r7nv5cowfrx4vsyf37" FOREIGN KEY ("ended_by") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "automation_rules"
    ADD CONSTRAINT "fklqrls4ropdbaxrtw84l291uiy" FOREIGN KEY ("conversation_id") REFERENCES "conversations"("conversation_id");

ALTER TABLE ONLY "messages"
    ADD CONSTRAINT "fknx80r8l5k61es69hurb3dbmnv" FOREIGN KEY ("from_account_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "conversation_members"
    ADD CONSTRAINT "fknxfbup81m9td8l03se3rg2icf" FOREIGN KEY ("conversation_id") REFERENCES "conversations"("conversation_id");

ALTER TABLE ONLY "conversation_members"
    ADD CONSTRAINT "fkosvpesom2hosqrhos0cf6uel0" FOREIGN KEY ("user_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "meetings"
    ADD CONSTRAINT "fkrtqx6qqm566talvut57jevx8e" FOREIGN KEY ("conversation_id") REFERENCES "conversations"("conversation_id");

ALTER TABLE ONLY "automation_executions"
    ADD CONSTRAINT "fksbnddh4p4yfbsxeky0geoun3o" FOREIGN KEY ("requested_by_user_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "friendships"
    ADD CONSTRAINT "fkt0mh1j446gu5rqba17rnknuil" FOREIGN KEY ("friend_id") REFERENCES "public"."users"("user_id");

ALTER TABLE ONLY "messages"
    ADD CONSTRAINT "fkt492th6wsovh1nush5yl5jj8e" FOREIGN KEY ("conversation_id") REFERENCES "conversations"("conversation_id");

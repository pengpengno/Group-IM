2. 聊天会话表 (conversations)
   记录单聊和群聊的基本信息。

字段	类型	描述
conversation_id	INT	会话 ID
conversation_type	ENUM('single', 'group')	会话类型（单聊或群聊）
created_by	INT	创建者用户 ID
created_at	DATETIME	创建时间
updated_at	DATETIME	最近更新时间
3. 群聊成员表 (group_members)
   群聊的成员信息。

字段	类型	描述
group_id	INT	群组 ID
user_id	INT	用户 ID
role	ENUM('admin', 'member')	角色（管理员或成员）
joined_at	DATETIME	加入时间
left_at	DATETIME	离开时间（可为空）
4. 消息表 (messages)
   记录每一条消息的详细信息，包括发送者、接收者、消息内容等。

字段	类型	描述
message_id	INT	消息 ID
conversation_id	INT	会话 ID（外键，关联到 conversations）
sender_id	INT	发送者用户 ID
receiver_id	INT	接收者用户 ID（单聊时为目标用户，群聊时为群聊会话）
message_type	ENUM('text', 'image', 'audio', 'video', 'file', 'emoji', 'link')	消息类型
content	TEXT	消息内容（文本、图片路径、文件路径等）
timestamp	DATETIME	消息发送时间
status	ENUM('sent', 'delivered', 'read', 'failed')	消息状态
is_deleted	BOOLEAN	是否已删除
is_recalled	BOOLEAN	是否已撤回
original_message_id	INT	如果是撤回的消息，则存储原消息 ID
5. 富文本表 (rich_texts)
   保存富文本消息的相关信息，如表情、图片等。

字段	类型	描述
rich_text_id	INT	富文本 ID
message_id	INT	消息 ID（外键，关联到 messages）
rich_text_type	ENUM('emoji', 'image', 'link', 'file')	富文本类型（表情、图片、链接、文件）
content	TEXT	富文本内容（例如表情的 Unicode 或图片的 URL）
6. 消息搜索索引表 (message_search_index)
   为了提高消息搜索效率，可以在此表中索引消息内容和关键词。

字段	类型	描述
message_id	INT	消息 ID（外键，关联到 messages）
keywords	TEXT	关键词索引（分词后的文本）
indexed_at	DATETIME	索引时间
7. 通话记录表 (calls)
   记录音视频通话的基本信息。

字段	类型	描述
call_id	INT	通话 ID
caller_id	INT	主叫用户 ID
receiver_id	INT	被叫用户 ID
call_type	ENUM('audio', 'video')	通话类型（音频或视频）
start_time	DATETIME	通话开始时间
end_time	DATETIME	通话结束时间
status	ENUM('completed', 'missed', 'failed')	通话状态
duration	INT	通话时长（秒）
8. 文件表 (files)
   记录消息中的文件内容。

字段	类型	描述
file_id	INT	文件 ID
sender_id	INT	发送者用户 ID
file_type	VARCHAR(50)	文件类型
file_url	VARCHAR(255)	文件 URL
file_size	INT	文件大小（单位：字节）
uploaded_at	DATETIME	上传时间
9. 表情表 (emojis)
   记录自定义表情的信息。

字段	类型	描述
emoji_id	INT	表情 ID
emoji_name	VARCHAR(50)	表情名称
emoji_url	VARCHAR(255)	表情图片的 URL
created_at	DATETIME	创建时间

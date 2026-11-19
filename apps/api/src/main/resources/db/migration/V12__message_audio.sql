-- Voice notes and audio files received from contacts. Text messages leave these null.
alter table messages
    add column audio_mime_type        varchar(100),
    add column audio_duration_seconds integer,
    add column audio_storage_provider varchar(40),
    add column audio_storage_key      text,
    add column transcription_status   varchar(20);

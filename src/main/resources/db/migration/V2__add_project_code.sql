-- 프로젝트 공개 코드. 접수 폼 링크(?project=)에 쓰는 외부용 식별자로,
-- 내부 PK(ULID)를 URL 에 노출하지 않기 위해 둔다.
ALTER TABLE projects ADD COLUMN code TEXT NOT NULL DEFAULT '';

-- 기존 행은 PK 뒷자리(랜덤 구간)로 임시 코드를 채운다. 생성 시각이 담긴 앞자리는 쓰지 않는다.
UPDATE projects SET code = 'p-' || lower(substr(id, -8)) WHERE code = '';

CREATE UNIQUE INDEX ux_projects_code ON projects (code);

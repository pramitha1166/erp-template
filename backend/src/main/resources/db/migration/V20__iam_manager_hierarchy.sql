-- WF-3: Epic 0.4's "approver resolution by reporting hierarchy" needs a
-- manager relationship on User, which IAM (Epic 0.2) never modeled -- it
-- had no requirement driving one at the time. Added here, additively,
-- rather than deferred: unlike branch_id (ORG-2) this isn't a column that
-- corrupts historic data if it arrived late, but workflow's hierarchy
-- resolver has nothing to walk without it.
ALTER TABLE users ADD COLUMN manager_id uuid REFERENCES users (id);

CREATE INDEX idx_users_manager_id ON users (manager_id);

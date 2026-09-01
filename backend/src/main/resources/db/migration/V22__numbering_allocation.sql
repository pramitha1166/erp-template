-- Epic 0.5 (PLAT-NUM): allocation support for the naming series V15
-- introduced at onboarding time. reset_policy/fiscal_year_start_month make
-- NUM-3's fiscal-year reset behaviour configurable per series;
-- current_period_key tracks which reset period next_counter currently
-- belongs to, so allocation (NUM-2/NUM-4, application-layer) can detect a
-- rollover and reset the counter before handing out the next number.
ALTER TABLE numbering_series
    ADD COLUMN reset_policy varchar(16) NOT NULL DEFAULT 'NEVER' CHECK (reset_policy IN ('NEVER', 'ANNUAL')),
    ADD COLUMN fiscal_year_start_month int NOT NULL DEFAULT 1 CHECK (fiscal_year_start_month BETWEEN 1 AND 12),
    ADD COLUMN current_period_key varchar(20);

alter table match_decisions
    add column purpose varchar(120) not null default 'candidate-job-matching';

alter table match_decisions alter column purpose drop default;

-- Migration script to move data from sgpa_results_csm to college-specific grades tables

-- Create the new tables if they don't exist
CREATE TABLE IF NOT EXISTS `srkit_grades_csm` (
  `htno` VARCHAR(20) PRIMARY KEY,
  `sem_1_1` DECIMAL(4,2),
  `sem_1_2` DECIMAL(4,2),
  `sem_2_1` DECIMAL(4,2),
  `sem_2_2` DECIMAL(4,2),
  `sem_3_1` DECIMAL(4,2),
  `sem_3_2` DECIMAL(4,2),
  `sem_4_1` DECIMAL(4,2),
  `sem_4_2` DECIMAL(4,2),
  `cgpa` DECIMAL(4,2)
);

CREATE TABLE IF NOT EXISTS `aleit_grades_csm` (
  `htno` VARCHAR(20) PRIMARY KEY,
  `sem_1_1` DECIMAL(4,2),
  `sem_1_2` DECIMAL(4,2),
  `sem_2_1` DECIMAL(4,2),
  `sem_2_2` DECIMAL(4,2),
  `sem_3_1` DECIMAL(4,2),
  `sem_3_2` DECIMAL(4,2),
  `sem_4_1` DECIMAL(4,2),
  `sem_4_2` DECIMAL(4,2),
  `cgpa` DECIMAL(4,2)
);

-- Migrate data from sgpa_results_csm to srkit_grades_csm for SRKIT students
INSERT INTO srkit_grades_csm (htno, sem_1_1, sem_1_2, sem_2_1, sem_2_2, sem_3_1, sem_3_2, sem_4_1, sem_4_2, cgpa)
SELECT s.htno, s.sem_1_1, s.sem_1_2, s.sem_2_1, s.sem_2_2, s.sem_3_1, s.sem_3_2, s.sem_4_1, s.sem_4_2, s.cgpa
FROM sgpa_results_csm s
JOIN srkit_students st ON s.htno = st.htno
ON DUPLICATE KEY UPDATE
  sem_1_1 = s.sem_1_1,
  sem_1_2 = s.sem_1_2,
  sem_2_1 = s.sem_2_1,
  sem_2_2 = s.sem_2_2,
  sem_3_1 = s.sem_3_1,
  sem_3_2 = s.sem_3_2,
  sem_4_1 = s.sem_4_1,
  sem_4_2 = s.sem_4_2,
  cgpa = s.cgpa;

-- Migrate data from sgpa_results_csm to aleit_grades_csm for ALEIT students
-- First, check if there are any ALEIT students in the results
INSERT INTO aleit_grades_csm (htno, sem_1_1, sem_1_2, sem_2_1, sem_2_2, sem_3_1, sem_3_2, sem_4_1, sem_4_2, cgpa)
SELECT s.htno, s.sem_1_1, s.sem_1_2, s.sem_2_1, s.sem_2_2, s.sem_3_1, s.sem_3_2, s.sem_4_1, s.sem_4_2, s.cgpa
FROM sgpa_results_csm s
WHERE EXISTS (
  SELECT 1 FROM srkit_results_csm_2_1 r
  WHERE s.htno = r.htno
  AND NOT EXISTS (SELECT 1 FROM srkit_students st WHERE s.htno = st.htno)
)
ON DUPLICATE KEY UPDATE
  sem_1_1 = s.sem_1_1,
  sem_1_2 = s.sem_1_2,
  sem_2_1 = s.sem_2_1,
  sem_2_2 = s.sem_2_2,
  sem_3_1 = s.sem_3_1,
  sem_3_2 = s.sem_3_2,
  sem_4_1 = s.sem_4_1,
  sem_4_2 = s.sem_4_2,
  cgpa = s.cgpa;

-- For any remaining students that don't match either college, check which college they belong to
-- This is a fallback to ensure no data is lost
INSERT INTO srkit_grades_csm (htno, sem_1_1, sem_1_2, sem_2_1, sem_2_2, sem_3_1, sem_3_2, sem_4_1, sem_4_2, cgpa)
SELECT s.htno, s.sem_1_1, s.sem_1_2, s.sem_2_1, s.sem_2_2, s.sem_3_1, s.sem_3_2, s.sem_4_1, s.sem_4_2, s.cgpa
FROM sgpa_results_csm s
WHERE NOT EXISTS (SELECT 1 FROM srkit_grades_csm sg WHERE s.htno = sg.htno)
AND NOT EXISTS (SELECT 1 FROM aleit_grades_csm ag WHERE s.htno = ag.htno)
ON DUPLICATE KEY UPDATE
  sem_1_1 = s.sem_1_1,
  sem_1_2 = s.sem_1_2,
  sem_2_1 = s.sem_2_1,
  sem_2_2 = s.sem_2_2,
  sem_3_1 = s.sem_3_1,
  sem_3_2 = s.sem_3_2,
  sem_4_1 = s.sem_4_1,
  sem_4_2 = s.sem_4_2,
  cgpa = s.cgpa;

-- After migration is complete and verified, you can drop the old table
-- DROP TABLE sgpa_results_csm;

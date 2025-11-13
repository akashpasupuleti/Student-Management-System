//package com.dailycodework.excel2database.repository;
//
//import com.dailycodework.excel2database.domain.Subject;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface SubjectRepository extends JpaRepository<Subject, Integer> {
//    Optional<Subject> findByHtnoAndSubcode(String htno, String subcode);
//    List<Subject> findByHtno(String htno);
//    boolean existsByHtno(String htno);
//}

package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {
    // QuerydslPredicateExecutor, Querydsl Web!!
    // 단점: 조인을 못함, parameter가 predicate라서 service or controller에서 Querydsl에 의존하는 형태가 발생될 수 있음.
    // 실무에서 쓰기 한계가 있음.
    List<Member> findByUsername(String username);
}

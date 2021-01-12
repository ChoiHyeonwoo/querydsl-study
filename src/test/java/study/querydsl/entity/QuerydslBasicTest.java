package study.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;   // 필드레벨로 가져가도 됨(동시성문제 고려 하지 않아도 됨)

    @BeforeEach
    void contextLoad(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL(){
        // test -> select * from member where username = member1
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQueryDSL(){
        // QMember member = new QMember("m"); -> 같은 테이블을 조인해야 할 때만 별칭으로 쓰고
        // 평상시에는 static import로 처리를 권장
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))    // 파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"), (member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        /*List<Member> members = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                    .select(member)
                    .fetchOne();


        Member fetchFirst = queryFactory
                .select(member)
                .fetchFirst();*/

        // 페이징 처리
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        results.getTotal(); // 전체 count -> total count 쿼리를 또 실행.
        List<Member> content = results.getResults(); // paging 처리 내용.

        // count
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * 1. 회원 나이 내림차순 정렬 (desc)
     * 2. 회원 이름 오름차순 (asc)
     * 이름이 없으면 마지막에 출력.
     */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4); // queryResults.getTotal() 실행 시 count 쿼리 실행
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2); // queryResults.getResults() 실행 시 select 쿼리 실행
    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

        //Tuple -> Data 형이 여러개를 조회할때 -> DTO를 많이 씀
    }

    /**
     *
     */
    @Test
    public void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀A에 소속되어잇는 모든 회원
     */
    @Test
    public void join1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타조인 : 연관관계 없이도 조인이 가능
     * 회원이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }
    /**
     * jpql: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for(Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인하기
     * 회원이름이 팀 이름과 같은 회원 외부조인
     */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name)) //보통은 .leftJoin(member.team) -> id로 조인이 된다.
                .where(member.username.eq(team.name))
                .fetch();

        for(Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void not_fetch_join(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        // 연관관계 lazy기 때문에 team은 로딩되지 앟음
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    public void fetch_join(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        // 패지조인 적용됨!
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장많은 멤버 조회
     */
    @Test
    public void subQuery(){
        // subquery alias
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.eq(
                                select(memberSub.age.max())  // JPAExpressions static import
                                .from(memberSub)
                        ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }
    /**
     * 평균나이 이상 멤버
     */
    @Test
    public void subQueryGoe(){
        // subquery alias
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.goe( // 평균 이상.
                                select(memberSub.age.avg())  // JPAExpressions static import
                                        .from(memberSub)
                        ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }
    /**
     * 10살 이상 멤버 (예제 적절 하진 않음.)
     */
    @Test
    public void subQueryIn(){
        // subquery alias
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.in( // 평균 이상.
                                select(memberSub.age)  // JPAExpressions static import
                                        .from(memberSub)
                                        .where(memberSub.age.gt(10))
                        ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg()) // JPAExpressions static import
                        .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("Tuple = " + tuple);
        }
    }

    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    @Test
    public void concat(){
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = "+ username);
            System.out.println("age = "+ age);
        }
    }

    @Test
    public void findDtoByJPQL(){
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age)  from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto dto : result) {
            String username = dto.getUsername();
            Integer age = dto.getAge();
            System.out.println("username = "+ username);
            System.out.println("age = "+ age);
        }
    }

    @Test
    public void findByQueryDsl(){   // Setter, Getter, Noarg anno 통해서
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto dto : result) {
            System.out.println("dto = "+ dto);
        }
    }

    @Test
    public void findByField(){ // Setter, Getter, Noarg anno 필요없이 필드에 꽂힘.
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto dto : result) {
            System.out.println("dto = "+ dto);
        }
    }

    @Test
    public void findByConstructor(){ // 생성자를 통해 꽂힘.
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto dto : result) {
            System.out.println("dto = "+ dto);
        }
    }

    @Test
    public void findUserDto(){ // as.
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        Expressions.as(
                                JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub), "age")))
                .from(member)
                .fetch();

        for (UserDto dto : result) {
            System.out.println("dto = "+ dto);
        }
    }

    /**
     * @QueryProjection 을 활용 (compile 시점에 쿼리 오류를 바로 잡을 수 있음.)
     */
    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {
        BooleanBuilder builder = new BooleanBuilder();
        //BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameParam)); -> usernameParam이 절대 null이 아닐때!

        if (usernameParam != null)
            builder.and(member.username.eq(usernameParam));

        if (ageParam != null)
            builder.and(member.age.eq(ageParam));

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    //BooleanBuilder 보다 더 깔끔하다!
    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                //.where(usernameEq(usernameParam), ageEq(ageParam))
                .where(allEq(usernameParam, ageParam)) //-> 가능하다.
                .fetch();
    }

    //아래처럼 재활용이 무궁무진, 조합도 가능!!
    //여러 조건들을 조합해서 여러쿼리에서 쓸 수 있다.
    private BooleanExpression usernameEq(String usernameParam) {
        return usernameParam == null ? null :  member.username.eq(usernameParam);
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam == null ? null : member.age.eq(ageParam);
    }

    private Predicate allEq(String usernameParam, Integer ageParam) {
        return usernameEq(usernameParam).and(ageEq(ageParam));
    }
}

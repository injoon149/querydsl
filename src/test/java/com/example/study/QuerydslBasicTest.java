package com.example.study;


import com.example.study.dto.MemberDto;
import com.example.study.dto.QMemberDto;
import com.example.study.dto.UserDto;
import com.example.study.entity.Member;
import com.example.study.entity.QMember;
import com.example.study.entity.QTeam;
import com.example.study.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.study.entity.QMember.member;
import static com.example.study.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    @BeforeEach
    public void before() {
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
    public void startJPQL() {
        String qlString = "select m from Member m " + "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                        .setParameter("username", "member1")
                                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl2() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void searchAndParam() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Member> result1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"), member.age.eq(10)).fetch();
        Assertions.assertThat(result1.size()).isEqualTo(1);
    }

    @Test
    public void sort() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
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
        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();


    }

    @Test
    public void paging1() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        Assertions.assertThat(queryResults.getTotal()).isEqualTo(4);
        Assertions.assertThat(queryResults.getLimit()).isEqualTo(2);
        Assertions.assertThat(queryResults.getOffset()).isEqualTo(1);
        Assertions.assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    public void group() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
        Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    //팀 A에 소속된 모든 회원
    @Test
    public void join() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = QMember.member;
        QTeam team = QTeam.team;

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    //세타 조인(연관관계가 없는 필드로 조인)
    //회원의 이름이 팀 이름과 같은 회원 조회
    @Test
    public void theta_join() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    //조인 대상 필터링
    //회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
    // select m, t from member m left join m.team t on t.name = 'teamA'
    @Test
    public void join_on_filtering() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for(Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    //연관관계 없는 엔티티 외부 조인
    //회원의 이름과 팀의 이름이 같은 대상 외부 조인
    //select m, t from Member m left join Team t on m.username = t.name
    @Test
    public void join_on_no_relation() throws Exception
    {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for(Tuple tuple : result) {
            System.out.println("t = " + tuple);
        }
    }

    //페치 조인 미적용
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 미적용").isFalse();

    }

    //페치 조인 적용
    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    //서브 쿼리
    //나이가 가장 많은 회원 조회
    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(40);
    }

    //서브쿼리 goe 사용
    //나이가 평균 나이 이상인 회원

    @Test
    public void subQueryGoe() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    //서브쿼리 여러 건 처리, in 사용
    @Test
    public void subQueryIn() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(20,30,40);
    }

    //select 절에 subquery
    @Test
    public void selectSubquery() throws Exception {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = queryFactory
                .select(member.username, JPAExpressions
                        .select(memberSub.age.avg()).from(memberSub))
                .from(member).fetch();

        for(Tuple tuple : fetch) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " + tuple.get(JPAExpressions.select(memberSub.age.avg()).from(memberSub)));
        }
    }


    @Test
    public void basicCase() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member).fetch();

        for(String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0,20)).then("0~20살")
                        .when(member.age.between(21,30)).then("21~30살")
                        .otherwise("기타"))
                .from(member).fetch();

        for(String s : result) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    public void constant() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member).fetch();
        for(Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    
    @Test
    public void concat() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        
        for(String s : result) {
            System.out.println("s = " + s);
        }
    }
    //프로젝션 대상이 하나
    @Test
    public void simpleProjection() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(member.username)
                .from(member).fetch();
        
        for(String s : result) {
            System.out.println("s = " + s);
        }
    }

    //프로젝션 대상이 둘 이상
    @Test
    public void tupleProjection() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member).fetch();

        for(Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("age = " + age);
            System.out.println("username = " + username);
        }
    }

    //프로퍼티 접근 - setter
    @Test
    public void findDtoBySetter() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member).fetch();
        
        for(MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //필드 직접 접근
    @Test
    public void findDtoByField() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member).fetch();

        for(MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    
    @Test
    public void findDtoByConstructor() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member).fetch();
        
        for(UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByQueryProjection() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member).fetch();

        for(MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void 동적쿼리_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder).fetch();
    }

    @Test
    public void 동적쿼리_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    @Test
    public void bulkUpdate() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        //member1 = 10 -> 비회원
        //member2 = 20 -> 비회원
        //member3 = 30 -> 유지
        //member4 = 40 -> 유지

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member).fetch();
        
        for(Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    public void bulkAdd() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkDelete() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        String result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member).fetchFirst();

        System.out.println("result = " + result);
    }

    @Test
    public void sqlFunction2() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        Expressions.stringTemplate("function('lower', {0})",
                                member.username
                ))).fetch();

        for(String s : result) {
            System.out.println("s = " + s);
        }
    }

    










}

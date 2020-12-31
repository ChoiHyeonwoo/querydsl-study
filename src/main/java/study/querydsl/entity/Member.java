package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter // Setter는 안쓰는게 좋다고?
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본생성자 생성
@ToString(of = {"id", "name", "age"})   // 연관관계 양방향 걸린 것은 걸지 말아야 함 -> recursive error
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // 외래키 제약조건이 되진 않음.
    private Team team;

    public Member(String username) {
        this(username,0);
    }

    public Member(String username, int age) {
       this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }

    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}

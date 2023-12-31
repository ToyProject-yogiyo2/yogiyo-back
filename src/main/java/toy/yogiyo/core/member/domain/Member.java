package toy.yogiyo.core.member.domain;

import lombok.*;
import toy.yogiyo.common.domain.BaseTimeEntity;
import toy.yogiyo.core.like.domain.Like;
import toy.yogiyo.core.address.domain.MemberAddress;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "Members")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
public class Member extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nickname;
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private ProviderType providerType;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberAddress> memberAddresses = new ArrayList<>();


    @Builder
    public Member(Long id, String nickname, String email, String password, ProviderType providerType, List<MemberAddress> memberAddresses) {
        this.id = id;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.providerType = providerType;
        this.memberAddresses = memberAddresses;
    }

    public void update(Member member){
        if(this.nickname != member.getNickname()) this.nickname = member.getNickname();
    }

    public void addLike(Like like){
        this.likes.add(like);
    }
    public void addMemberAddresses(MemberAddress memberAddress){
        this.memberAddresses.forEach(memberAddress1 -> memberAddress1.isHere(false));
        this.memberAddresses.add(memberAddress);
        memberAddress.setMember(this);
    }

    public void setHere(Long memberAddressId){
        this.memberAddresses
                .forEach(memberAddress -> memberAddress.isHere(memberAddress.getId().equals(memberAddressId)));
    }
    public void setEncodedPassword(String password){
        this.password = password;
    }
}

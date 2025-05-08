ackage com.example.chatapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

@Entity
@Table(name = "chat_groups")
public class ChatGrp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;


    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private ChatUser creator;

    @Column(name = "created_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @ManyToMany
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<ChatUser> members = new HashSet<>();

    // Default constructor required by Hibernate
    public ChatGrp() {
    }

    public ChatGrp(String name, String description, ChatUser creator) {
        this.name = name;
        this.description = description;
        this.creator = creator;
        this.createdDate = new Date();
        this.members.add(creator); // Creator is automatically a member
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ChatUser getCreator() {
        return creator;
    }

    public void setCreator(ChatUser creator) {
        this.creator = creator;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Set<ChatUser> getMembers() {
        return members;
    }

    public void setMembers(Set<ChatUser> members) {
        this.members = members;
    }

    public void addMember(ChatUser user) {
        this.members.add(user);
    }

    public void removeMember(ChatUser user) {
        this.members.remove(user);
    }

    public boolean isMember(ChatUser user) {
        return this.members.contains(user);
    }

    @Override
    public String toString() {
        return "ChatGrp{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", creator=" + (creator != null ? creator.getUsername() : "null") +
                ", createdDate=" + createdDate +
                ", memberCount=" + members.size() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatGrp chatGrp = (ChatGrp) o;
        return Objects.equals(id, chatGrp.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}



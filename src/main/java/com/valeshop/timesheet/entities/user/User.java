package com.valeshop.timesheet.entities.user;

import com.valeshop.timesheet.entities.demands.DemandRecord;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "tb_users")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@Inheritance(strategy = InheritanceType.JOINED)
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    protected String email;
    protected String password;
    protected Integer userType;

    @OneToMany(mappedBy = "user")
    private final Set<DemandRecord> demand = new HashSet<>();

    public User(Long id, String email, String password, UserType userType) {
        this.id = id;
        this.email = email;
        this.password = password;
        setUserType(userType);
    }

    public UserType getUserType() {
        return UserType.valueOf(userType);
    }

    public void setUserType(UserType userType) {
        if (userType != null) this.userType = userType.getCode();
    }

}

package com.valeshop.timesheet.entities.demands;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.valeshop.timesheet.entities.user.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "demands")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@Inheritance(strategy = InheritanceType.JOINED)
public class DemandRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected long id;
    protected String title;
    protected String gitLink;
    protected Integer priority;
    protected String status;
    protected Date date;
    protected String description;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "id_user")
    private User user; // a relação dependente leva a coluna no construtor

    public DemandRecord(long id, String title, String gitLink, Integer priority, String status, Date date, String description, User user) {
        this.id = id;
        this.title = title;
        this.gitLink = gitLink;
        this.priority = priority;
        this.status = status;
        this.date = date;
        this.description = description;
        this.user = user;
    }
}

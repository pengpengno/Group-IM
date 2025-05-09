package com.github.im.group.gui.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Id;

@Entity
@Table(name = "user_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoTable {

    @Id
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String userName;

    @Column(length = 20)
    private String telephone;

    @Column(length = 100)
    private String email;

    // 可以补充更多字段，如头像、简介等

}

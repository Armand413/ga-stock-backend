package com.ga.gestionstock;

import com.ga.gestionstock.config.AdUserMapper;
import com.ga.gestionstock.entity.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.assertj.core.api.Assertions.*;

class AdUserMapperTests {
    static final String USER_GROUP = "CN=Consommables-Users,OU=Groups,DC=ga,DC=test";
    static final String ADMIN_GROUP = "CN=Consommables-Admins,OU=Groups,DC=ga,DC=test";
    final AdUserMapper mapper = new AdUserMapper(USER_GROUP, ADMIN_GROUP);

    DirContextAdapter contexte(String groupe) {
        return contexte(groupe, "agent@example.test", new byte[16]);
    }

    DirContextAdapter contexte(String groupe, String email, byte[] guid) {
        var ctx = new DirContextAdapter();
        if (guid != null) ctx.setAttributeValue("objectGUID", guid);
        ctx.setAttributeValue("userPrincipalName", "Agent@ga.test");
        ctx.setAttributeValue("displayName", "Agent de test");
        if (email != null) ctx.setAttributeValue("mail", email);
        ctx.setAttributeValues("memberOf", new Object[]{groupe});
        return ctx;
    }

    @Test
    void seulsLesGroupesCompletsConfigurésDeterminentLeRole() {
        var simple = (AdUserMapper.AdPrincipal) mapper.mapUserFromContext(contexte(USER_GROUP), "agent", List.of());
        assertThat(simple.identite().role()).isEqualTo(Role.LECTEUR);
        assertThat(simple.identite().identifiant()).isEqualTo("agent@ga.test");
        assertThat(simple.getPassword()).isNull();
        var admin = (AdUserMapper.AdPrincipal) mapper.mapUserFromContext(contexte(ADMIN_GROUP.toUpperCase()), "agent", List.of());
        assertThat(admin.identite().role()).isEqualTo(Role.ADMIN);
        assertThatThrownBy(() -> mapper.mapUserFromContext(contexte("CN=Domain Admins,DC=ga,DC=test"), "agent",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void emailEtIdentiteStablesObligatoires() {
        var missingEmail = contexte(USER_GROUP, null, new byte[16]);
        assertThatThrownBy(() -> mapper.mapUserFromContext(missingEmail, "agent", List.of())).isInstanceOf(BadCredentialsException.class);
        var missingGuid = contexte(USER_GROUP, "agent@example.test", null);
        assertThatThrownBy(() -> mapper.mapUserFromContext(missingGuid, "agent", List.of())).isInstanceOf(BadCredentialsException.class);
    }
}

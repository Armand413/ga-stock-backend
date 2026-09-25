package com.ga.gestionstock.config;

import com.ga.gestionstock.dto.IdentiteAnnuaire;
import com.ga.gestionstock.entity.Role;
import jakarta.mail.internet.InternetAddress;
import java.util.*;
import org.springframework.ldap.core.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

public class AdUserMapper implements UserDetailsContextMapper {
    private final String groupeUtilisateurs;
    private final String groupeAdministrateurs;

    public AdUserMapper(String groupeUtilisateurs, String groupeAdministrateurs) {
        this.groupeUtilisateurs = groupeUtilisateurs;
        this.groupeAdministrateurs = groupeAdministrateurs;
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                         Collection<? extends GrantedAuthority> authorities) {
        String[] groupes = ctx.getStringAttributes("memberOf");
        var memberships = groupes == null ? List.<String>of() : Arrays.asList(groupes);
        boolean admin = memberships.stream().anyMatch(groupeAdministrateurs::equalsIgnoreCase);
        if (!admin && memberships.stream().noneMatch(groupeUtilisateurs::equalsIgnoreCase))
            throw new BadCredentialsException("Compte non autorise pour cette application.");
        Object guid = ctx.getObjectAttribute("objectGUID");
        if (!(guid instanceof byte[] bytes) || bytes.length != 16)
            throw new BadCredentialsException("Identite annuaire incomplete.");
        String upn = ctx.getStringAttribute("userPrincipalName");
        String nom = ctx.getStringAttribute("displayName");
        String email = ctx.getStringAttribute("mail");
        if (upn == null || upn.isBlank() || upn.length() > 254)
            throw new BadCredentialsException("Identite annuaire incomplete.");
        if (nom == null || nom.isBlank()) nom = upn;
        if (nom.length() > 150) nom = nom.substring(0, 150);
        try {
            if (email == null || email.length() > 254 || email.contains("\r") || email.contains("\n"))
                throw new IllegalArgumentException();
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            if (!address.getAddress().equals(email)) throw new IllegalArgumentException();
        } catch (Exception ex) { throw new BadCredentialsException("Adresse mail AD absente ou invalide."); }
        return new AdPrincipal(new IdentiteAnnuaire(Base64.getEncoder().encodeToString(bytes),
                upn.toLowerCase(Locale.ROOT), nom, email, admin ? Role.ADMIN : Role.LECTEUR));
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException("L'annuaire est en lecture seule.");
    }

    public record AdPrincipal(IdentiteAnnuaire identite) implements UserDetails {
        @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
        @Override public String getPassword() { return null; }
        @Override public String getUsername() { return identite.identifiant(); }
    }
}

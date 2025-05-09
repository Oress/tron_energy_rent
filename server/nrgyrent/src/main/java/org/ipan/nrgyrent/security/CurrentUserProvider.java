package org.ipan.nrgyrent.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Lookup;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserProvider {
    public CurrentUserInfo getCurrentUser() {
        return null;
/*
        EntityManager em = getEntityManager();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserProfile> criteriaQuery = cb.createQuery(UserProfile.class);
        Root<UserProfile> root = criteriaQuery.from(UserProfile.class);
        Join<UserProfile, UserAuth> join = root.join(UserProfile_.auth);
        criteriaQuery.where(cb.equal(join.get(UserAuth_.USERNAME), userDetails.getUsername()));
        TypedQuery<UserProfile> query = em.createQuery(criteriaQuery);

        query.setHint("org.hibernate.comment", "Get current user profile");

        UserProfile userProfile = query.getSingleResult();

        return new CurrentUserInfo(){
            @Override
            public UserProfile getUser() {
                return userProfile;
            }
        };
*/
    }

    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}

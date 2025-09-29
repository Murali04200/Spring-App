package com.example.keycloak.teacherverification;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.UserModel;

/**
 * Custom Keycloak authenticator that verifies a teacher-specific attribute on the user profile.
 */
public class TeacherVerificationAuthenticator implements Authenticator {

    private static final Logger LOGGER = Logger.getLogger(TeacherVerificationAuthenticator.class);
    static final String PROVIDER_ID = "teacher-verification-authenticator";
    private static final String TEACHER_VERIFIED_ATTRIBUTE = "teacherVerified";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            LOGGER.warn("Teacher verification attempted without an authenticated user in context.");
            context.attempted();
            return;
        }

        String teacherFlag = user.getFirstAttribute(TEACHER_VERIFIED_ATTRIBUTE);
        if (teacherFlag != null && Boolean.parseBoolean(teacherFlag)) {
            LOGGER.debugf("Teacher verification succeeded for user %s.", user.getUsername());
            context.success();
            return;
        }

        LOGGER.debugf("Teacher verification failed for user %s. Attribute '%s' missing or false.",
                user.getUsername(), TEACHER_VERIFIED_ATTRIBUTE);
        Response challenge = context.form()
                .setError("Teacher verification failed. Contact your administrator.")
                .createErrorPage(Response.Status.FORBIDDEN);
        context.failureChallenge(AuthenticationFlowError.ACCESS_DENIED, challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // No form submission is expected; simply re-run the verification.
        authenticate(context);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(org.keycloak.models.KeycloakSession session, org.keycloak.models.RealmModel realm, UserModel user) {
        String teacherFlag = user.getFirstAttribute(TEACHER_VERIFIED_ATTRIBUTE);
        return teacherFlag != null && !teacherFlag.isBlank();
    }

    @Override
    public void setRequiredActions(org.keycloak.models.KeycloakSession session, org.keycloak.models.RealmModel realm, UserModel user) {
        // No required actions; enforcement happens directly in this authenticator.
    }

    @Override
    public void close() {
        // Nothing to close.
    }
}

package com.example.keycloak.teacherverification;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class TeacherVerificationAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = TeacherVerificationAuthenticator.PROVIDER_ID;
    private static final TeacherVerificationAuthenticator INSTANCE = new TeacherVerificationAuthenticator();
    private static final List<AuthenticationExecutionModel.Requirement> REQUIREMENT_CHOICES = List.of(
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    );

    @Override
    public Authenticator create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization required.
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to do.
    }

    @Override
    public void close() {
        // Nothing to close.
    }

    @Override
    public String getReferenceCategory() {
        return "teacher-verification";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES.toArray(AuthenticationExecutionModel.Requirement[]::new);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public String getDisplayType() {
        return "Teacher Verification";
    }

    @Override
    public String getHelpText() {
        return "Checks if the user has the teacherVerified attribute set to true.";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

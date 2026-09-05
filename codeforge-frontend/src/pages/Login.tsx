import { Button, Link as MuiLink, Stack, TextField } from '@mui/material';
import { useState, type SubmitEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate } from 'react-router';
import { useApiErrors } from '../api/use-api-errors';
import { useAuth } from '../auth/use-auth';
import { AuthLayout } from '../components/auth/AuthLayout';
import { useMessages } from '../i18n/use-messages';
import { paths } from '../routes/paths';

export const LoginPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { fieldErrors, generalError, capture, reset, clearField } = useApiErrors();

  const redirectTo = (location.state as { from?: string } | null)?.from ?? paths.home;

  const handleSubmit = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    reset();
    setSubmitting(true);

    try {
      await login({ username, password });
      void navigate(redirectTo, { replace: true });
    } catch (error) {
      capture(error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      title={t('auth.login.title')}
      subtitle={t('auth.login.subtitle')}
      errorMessage={generalError ? message(generalError.code, generalError.message) : undefined}
      footer={
        <>
          {t('auth.login.noAccount')}{' '}
          <MuiLink component={Link} to={paths.register}>
            {t('auth.login.signUpLink')}
          </MuiLink>
        </>
      }
    >
      <Stack component="form" spacing={2.5} onSubmit={handleSubmit} noValidate>
        <TextField
          label={t('auth.fields.username')}
          value={username}
          onChange={(event) => {
            setUsername(event.target.value);
            clearField('username');
          }}
          error={Boolean(fieldErrors.username)}
          helperText={fieldErrors.username ? message(fieldErrors.username) : ' '}
          autoComplete="username"
          autoFocus
          fullWidth
        />

        <TextField
          label={t('auth.fields.password')}
          type="password"
          value={password}
          onChange={(event) => {
            setPassword(event.target.value);
            clearField('password');
          }}
          error={Boolean(fieldErrors.password)}
          helperText={fieldErrors.password ? message(fieldErrors.password) : ' '}
          autoComplete="current-password"
          fullWidth
        />

        <Button type="submit" size="large" disabled={submitting} fullWidth>
          {submitting ? t('auth.login.submitting') : t('auth.login.submit')}
        </Button>
      </Stack>
    </AuthLayout>
  );
};

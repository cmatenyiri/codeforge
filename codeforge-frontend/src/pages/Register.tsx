import { Button, Link as MuiLink, Stack, TextField } from '@mui/material';
import { useState, type SubmitEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { useApiErrors } from '../api/use-api-errors';
import { useAuth } from '../auth/use-auth';
import { AuthLayout } from '../components/auth/AuthLayout';
import { AvatarPicker } from '../components/user/AvatarPicker';
import { type AvatarId } from '../components/user/avatars';
import { useMessages } from '../i18n/use-messages';
import { paths } from '../routes/paths';

export const RegisterPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({ username: '', email: '', password: '', confirmPassword: '', avatar: '' });
  const [submitting, setSubmitting] = useState(false);
  const { fieldErrors, generalError, capture, reset, clearField } = useApiErrors();

  const updateField = (field: keyof typeof form) => (event: { target: { value: string } }) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    clearField(field);
  };

  const handleSubmit = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    reset();
    setSubmitting(true);

    try {
      await register(form);
      void navigate(paths.home, { replace: true });
    } catch (error) {
      capture(error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      title={t('auth.register.title')}
      subtitle={t('auth.register.subtitle')}
      errorMessage={generalError ? message(generalError.code, generalError.message) : undefined}
      footer={
        <>
          {t('auth.register.haveAccount')}{' '}
          <MuiLink component={Link} to={paths.login}>
            {t('auth.register.signInLink')}
          </MuiLink>
        </>
      }
    >
      <Stack component="form" spacing={2.5} onSubmit={handleSubmit} noValidate>
        <TextField
          label={t('auth.fields.username')}
          value={form.username}
          onChange={updateField('username')}
          error={Boolean(fieldErrors.username)}
          helperText={fieldErrors.username ? message(fieldErrors.username) : t('auth.hints.username')}
          autoComplete="username"
          autoFocus
          fullWidth
        />

        <TextField
          label={t('auth.fields.email')}
          type="email"
          value={form.email}
          onChange={updateField('email')}
          error={Boolean(fieldErrors.email)}
          helperText={fieldErrors.email ? message(fieldErrors.email) : ' '}
          autoComplete="email"
          fullWidth
        />

        <TextField
          label={t('auth.fields.password')}
          type="password"
          value={form.password}
          onChange={updateField('password')}
          error={Boolean(fieldErrors.password)}
          helperText={fieldErrors.password ? message(fieldErrors.password) : t('auth.hints.password')}
          autoComplete="new-password"
          fullWidth
        />

        <TextField
          label={t('auth.fields.confirmPassword')}
          type="password"
          value={form.confirmPassword}
          onChange={updateField('confirmPassword')}
          error={Boolean(fieldErrors.confirmPassword)}
          helperText={fieldErrors.confirmPassword ? message(fieldErrors.confirmPassword) : ' '}
          autoComplete="new-password"
          fullWidth
        />

        <AvatarPicker
          value={form.avatar === '' ? null : (form.avatar as AvatarId)}
          error={fieldErrors.avatar ? message(fieldErrors.avatar) : undefined}
          onChange={(avatar) => {
            setForm((current) => ({ ...current, avatar }));
            clearField('avatar');
          }}
        />

        <Button type="submit" size="large" disabled={submitting} fullWidth>
          {submitting ? t('auth.register.submitting') : t('auth.register.submit')}
        </Button>
      </Stack>
    </AuthLayout>
  );
};

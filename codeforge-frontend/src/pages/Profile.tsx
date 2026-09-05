import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded';
import { Alert, Box, Button, Container, Link as MuiLink, Paper, Stack, TextField, Typography } from '@mui/material';
import { useState, type SubmitEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useApiErrors } from '../api/use-api-errors';
import { usersApi } from '../api/users-api';
import { useAuth } from '../auth/use-auth';
import { AvatarPicker } from '../components/user/AvatarPicker';
import { isAvatarId, type AvatarId } from '../components/user/avatars';
import { useMessages } from '../i18n/use-messages';
import { paths } from '../routes/paths';

/** A titled panel with its own success banner and error handling. */
const Section = ({
  title,
  description,
  success,
  error,
  children,
}: {
  title: string;
  description?: string;
  success?: string;
  error?: string;
  children: React.ReactNode;
}) => (
  <Paper variant="outlined" sx={{ p: 3 }}>
    <Stack spacing={2}>
      <Box>
        <Typography variant="h4">{title}</Typography>
        {description ? (
          <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
            {description}
          </Typography>
        ) : null}
      </Box>
      {error ? <Alert severity="error">{error}</Alert> : null}
      {success ? <Alert severity="success">{success}</Alert> : null}
      {children}
    </Stack>
  </Paper>
);

const AvatarSection = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const { user, applyUser } = useAuth();
  const { fieldErrors, generalError, capture, reset } = useApiErrors();
  const [saved, setSaved] = useState(false);

  const current = user && isAvatarId(user.avatar) ? user.avatar : null;

  const choose = async (avatar: AvatarId) => {
    reset();
    setSaved(false);
    try {
      applyUser(await usersApi.updateAvatar({ avatar }));
      setSaved(true);
    } catch (error) {
      capture(error);
    }
  };

  return (
    <Section
      title={t('profile.avatarSection')}
      success={saved ? t('avatar.saved') : undefined}
      error={generalError ? message(generalError.code, generalError.message) : undefined}
    >
      <AvatarPicker
        value={current}
        label={t('avatar.current')}
        error={fieldErrors.avatar ? message(fieldErrors.avatar) : undefined}
        onChange={(avatar) => {
          void choose(avatar);
        }}
      />
    </Section>
  );
};

const UsernameSection = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const { user, applyUser } = useAuth();
  const { fieldErrors, generalError, capture, reset, clearField } = useApiErrors();
  const [username, setUsername] = useState(user?.username ?? '');
  const [submitting, setSubmitting] = useState(false);
  const [saved, setSaved] = useState(false);

  const submit = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    reset();
    setSaved(false);
    setSubmitting(true);
    try {
      applyUser(await usersApi.updateUsername({ username }));
      setSaved(true);
    } catch (error) {
      capture(error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Section
      title={t('profile.usernameSection')}
      description={t('profile.usernameHelp')}
      success={saved ? t('profile.usernameSaved') : undefined}
      error={generalError ? message(generalError.code, generalError.message) : undefined}
    >
      <Stack component="form" spacing={2} onSubmit={submit} noValidate sx={{ alignItems: 'flex-start' }}>
        <TextField
          label={t('auth.fields.username')}
          value={username}
          onChange={(event) => {
            setUsername(event.target.value);
            clearField('username');
            setSaved(false);
          }}
          error={Boolean(fieldErrors.username)}
          helperText={fieldErrors.username ? message(fieldErrors.username) : ' '}
          sx={{ maxWidth: 320, width: '100%' }}
        />
        <Button type="submit" disabled={submitting || username === user?.username}>
          {submitting ? t('profile.saving') : t('profile.saveUsername')}
        </Button>
      </Stack>
    </Section>
  );
};

const PasswordSection = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const { fieldErrors, generalError, capture, reset, clearField } = useApiErrors();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [submitting, setSubmitting] = useState(false);
  const [saved, setSaved] = useState(false);

  const update = (field: keyof typeof form) => (event: { target: { value: string } }) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    clearField(field);
    setSaved(false);
  };

  const submit = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    reset();
    setSaved(false);
    setSubmitting(true);
    try {
      await usersApi.updatePassword(form);
      setForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setSaved(true);
    } catch (error) {
      capture(error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Section
      title={t('profile.passwordSection')}
      description={t('profile.passwordHelp')}
      success={saved ? t('profile.passwordSaved') : undefined}
      error={generalError ? message(generalError.code, generalError.message) : undefined}
    >
      <Stack component="form" spacing={2} onSubmit={submit} noValidate sx={{ alignItems: 'flex-start' }}>
        {(['currentPassword', 'newPassword', 'confirmPassword'] as const).map((field) => (
          <TextField
            key={field}
            type="password"
            label={t(`auth.fields.${field}`)}
            value={form[field]}
            onChange={update(field)}
            error={Boolean(fieldErrors[field])}
            helperText={fieldErrors[field] ? message(fieldErrors[field]) : ' '}
            autoComplete={field === 'currentPassword' ? 'current-password' : 'new-password'}
            sx={{ maxWidth: 320, width: '100%' }}
          />
        ))}
        <Button type="submit" disabled={submitting}>
          {submitting ? t('profile.saving') : t('profile.savePassword')}
        </Button>
      </Stack>
    </Section>
  );
};

export const ProfilePage = () => {
  const { t } = useTranslation();

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <Container maxWidth="md" sx={{ py: 6 }}>
        <Stack spacing={3}>
          <Box>
            <MuiLink
              component={Link}
              to={paths.home}
              sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, mb: 1.5 }}
            >
              <ArrowBackRounded sx={{ fontSize: 16 }} />
              {t('profile.back')}
            </MuiLink>
            <Typography variant="h1">{t('profile.title')}</Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
              {t('profile.subtitle')}
            </Typography>
          </Box>

          <AvatarSection />
          <UsernameSection />
          <PasswordSection />
        </Stack>
      </Container>
    </Box>
  );
};

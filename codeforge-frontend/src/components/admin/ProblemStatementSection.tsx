import { useTranslation } from 'react-i18next';
import { FormSection } from './FormSection';
import { MarkdownField } from './MarkdownField';
import { type ProblemFormState } from './problem-form';

type ProblemStatementSectionProps = {
  form: ProblemFormState;
  onChange: (patch: Partial<ProblemFormState>) => void;
  errorOf: (field: string) => string | undefined;
};

/**
 * The problem as a solver reads it.
 *
 * <p>Constraints are a separate field rather than a heading inside the
 * description because the solving page renders them as their own block — and
 * because they are the part that has to stay honest about the hidden test cases
 * a submission is actually judged against.
 */
export const ProblemStatementSection = ({ form, onChange, errorOf }: ProblemStatementSectionProps) => {
  const { t } = useTranslation();

  return (
    <FormSection title={t('admin.form.statement')} description={t('admin.form.statementHelp')}>
      <MarkdownField
        label={t('admin.form.description')}
        value={form.description}
        onChange={(description) => {
          onChange({ description });
        }}
        error={errorOf('description')}
        placeholder={t('admin.form.descriptionPlaceholder')}
        rows={12}
      />

      <MarkdownField
        label={t('admin.form.constraints')}
        value={form.constraintsMarkdown}
        onChange={(constraintsMarkdown) => {
          onChange({ constraintsMarkdown });
        }}
        error={errorOf('constraintsMarkdown')}
        helperText={t('admin.form.constraintsHelp')}
        placeholder={t('admin.form.constraintsPlaceholder')}
        rows={6}
      />
    </FormSection>
  );
};

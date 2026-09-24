import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useLocation, useNavigate } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { salesApi } from '../api/endpoints.ts';
import { errorMessage } from '../api/messages.ts';
import { useAuth } from '../auth/AuthContext.tsx';
import { Alert, Button, Card, Field, Input } from '../components/ui.tsx';
import { EMAIL_REGEX } from '../lib/email.ts';

const schema = z.object({
  email: z.string().trim().regex(EMAIL_REGEX, 'Informe um e-mail válido'),
  password: z.string().min(1, 'Informe a senha'),
});
type LoginForm = z.infer<typeof schema>;

export function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({ resolver: zodResolver(schema) });
  const login = useMutation({
    mutationFn: (form: LoginForm) => salesApi.login(form.email, form.password),
    onSuccess: (auth) => {
      signIn(auth);
      navigate(from, { replace: true });
    },
  });

  return (
    <div className="mx-auto max-w-sm">
      <Card title="Entrar">
        <form className="flex flex-col gap-4" onSubmit={handleSubmit((form) => login.mutate(form))} noValidate>
          <Field label="E-mail" htmlFor="email" error={errors.email?.message}>
            <Input id="email" type="email" autoComplete="email" {...register('email')} />
          </Field>
          <Field label="Senha" htmlFor="password" error={errors.password?.message}>
            <Input id="password" type="password" autoComplete="current-password" {...register('password')} />
          </Field>
          {login.isError && <Alert>{errorMessage(login.error)}</Alert>}
          <Button type="submit" disabled={login.isPending}>
            {login.isPending ? 'Entrando...' : 'Entrar'}
          </Button>
          <p className="text-center text-sm text-slate-600">
            Ainda não tem cadastro?{' '}
            <Link to="/cadastro" className="font-medium text-indigo-700 underline">
              Cadastre-se
            </Link>
          </p>
        </form>
      </Card>
    </div>
  );
}

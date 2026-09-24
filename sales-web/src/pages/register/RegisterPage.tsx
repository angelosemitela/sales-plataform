import { useEffect, useMemo, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router';
import { useMutation, useQuery } from '@tanstack/react-query';
import { salesApi } from '../../api/endpoints.ts';
import { ApiError } from '../../api/client.ts';
import { errorMessage } from '../../api/messages.ts';
import type { Country, ZipCodeAddress } from '../../api/types.ts';
import { useAuth } from '../../auth/AuthContext.tsx';
import { Alert, Button, Card, Checkbox, Field, Input, Select } from '../../components/ui.tsx';
import { onlyDigits } from '../../lib/cpf.ts';
import { registerDefaults, registerSchema, toRegisterRequest, type RegisterForm } from './registerSchema.ts';

type AutoField = 'addressName' | 'district' | 'city' | 'state';
const AUTO_FIELDS: AutoField[] = ['addressName', 'district', 'city', 'state'];
type ZipStatus = 'idle' | 'loading' | 'found' | 'error';

const ADDRESS_TYPES = [
  { value: 'RESIDENCIAL', label: 'Residencial' },
  { value: 'COMERCIAL', label: 'Comercial' },
  { value: 'OTHER', label: 'Outro' },
] as const;

/** Campos "desabilitados" usam readOnly: o valor continua indo no envio do formulário. */
const lockedStyle = 'bg-slate-100 text-slate-500';

export function RegisterPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const countries = useQuery({ queryKey: ['countries'], queryFn: salesApi.countries, staleTime: Infinity });
  const documentTypes = useQuery({ queryKey: ['documentTypes'], queryFn: salesApi.documentTypes, staleTime: Infinity });

  const {
    register, control, handleSubmit, watch, setValue, setError, getValues,
    formState: { errors },
  } = useForm<RegisterForm>({ resolver: zodResolver(registerSchema), defaultValues: registerDefaults, mode: 'onBlur' });

  const countryName = useMemo(() => {
    const byCode = new Map((countries.data ?? []).map((c) => [c.code, c.name]));
    return (code: string) => byCode.get(code) ?? code;
  }, [countries.data]);

  // ---------------- Documento: tipo x país (regra 1.1.2) ----------------
  const documentType = watch('documentType');
  const selectedType = documentTypes.data?.find((t) => t.code === documentType);
  const documentCountryOptions: Country[] = useMemo(() => {
    if (!selectedType || !countries.data) return [];
    return selectedType.allCountries
      ? countries.data
      : countries.data.filter((c) => selectedType.allowedCountries.includes(c.code));
  }, [selectedType, countries.data]);

  useEffect(() => {
    if (!selectedType || documentCountryOptions.length === 0) return;
    const current = getValues('documentCountry');
    if (!documentCountryOptions.some((c) => c.code === current)) {
      // Troca de tipo: país atual não vale mais -> BR (se permitido) ou o 1º da lista.
      const next = documentCountryOptions.find((c) => c.code === 'BR') ?? documentCountryOptions[0];
      setValue('documentCountry', next.code);
    }
    if (selectedType.code === 'CPF') {
      setValue('documentValue', onlyDigits(getValues('documentValue'), 11));
    }
  }, [selectedType, documentCountryOptions, getValues, setValue]);

  // ---------------- Endereço: nacional x internacional + CEP (regra 1.1.3) ----------------
  const international = watch('international');
  const knowsZipCode = watch('knowsZipCode');
  const zipCode = watch('zipCode');
  const [zipStatus, setZipStatus] = useState<ZipStatus>('idle');
  const [zipMessage, setZipMessage] = useState<string | null>(null);
  const [filledByZip, setFilledByZip] = useState<Set<AutoField>>(new Set());
  const autoLookup = !international && knowsZipCode;

  useEffect(() => {
    if (!international) {
      setValue('addressCountry', 'BR'); // país sempre BR (e travado) em endereço nacional
    }
  }, [international, setValue]);

  useEffect(() => {
    if (!autoLookup) {
      setZipStatus('idle');
      setFilledByZip(new Set());
      return;
    }
    setZipMessage(null);
    if (!/^\d{8}$/.test(zipCode)) {
      setZipStatus('idle');
      return;
    }
    let cancelled = false; // evita aplicar a resposta de uma consulta antiga
    setZipStatus('loading');
    salesApi
      .zipCode(zipCode)
      .then((address: ZipCodeAddress) => {
        if (cancelled) return;
        const filled = new Set<AutoField>();
        for (const field of AUTO_FIELDS) {
          const value = address[field];
          setValue(field, value ?? '', { shouldValidate: value !== null });
          if (value !== null) filled.add(field);
        }
        setFilledByZip(filled);
        setZipStatus('found');
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        AUTO_FIELDS.forEach((field) => setValue(field, ''));
        setFilledByZip(new Set());
        setZipStatus('error');
        setZipMessage(errorMessage(error));
      });
    return () => {
      cancelled = true;
    };
  }, [autoLookup, zipCode, setValue]);

  /**
   * Um campo automático fica travado quando:
   *  - a consulta automática está ativa e ainda não respondeu (será preenchido), ou
   *  - a consulta respondeu COM valor para ele.
   * Campos que a consulta não trouxe (ou se ela falhou) ficam liberados.
   */
  const isLocked = (field: AutoField) =>
    autoLookup && (zipStatus === 'idle' || zipStatus === 'loading' || filledByZip.has(field));

  // ---------------- Envio ----------------
  const registerMutation = useMutation({
    mutationFn: (form: RegisterForm) => salesApi.register(toRegisterRequest(form)),
    onSuccess: (auth) => {
      signIn(auth);
      navigate('/', { replace: true });
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        if (error.code === 'EMAIL_ALREADY_REGISTERED') setError('email', { message: errorMessage(error) });
        if (error.code === 'INVALID_CPF' || error.code === 'DOCUMENT_ALREADY_REGISTERED') {
          setError('documentValue', { message: errorMessage(error) });
        }
      }
    },
  });

  if (countries.isPending || documentTypes.isPending) {
    return <p>Carregando...</p>;
  }
  if (countries.isError || documentTypes.isError) {
    return <Alert>Não foi possível carregar os dados do cadastro. Tente novamente.</Alert>;
  }

  const isCpf = documentType === 'CPF';

  return (
    <form className="mx-auto flex max-w-2xl flex-col gap-6" noValidate
          onSubmit={handleSubmit((form) => registerMutation.mutate(form))}>
      <h1 className="text-2xl font-semibold">Cadastro</h1>

      <Card title="Dados de acesso">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Field label="Nome completo" htmlFor="name" error={errors.name?.message}>
              <Input id="name" autoComplete="name" {...register('name')} />
            </Field>
          </div>
          <div className="sm:col-span-2">
            <Field label="E-mail" htmlFor="email" error={errors.email?.message}>
              <Input id="email" type="email" autoComplete="email" {...register('email')} />
            </Field>
          </div>
          <Field label="Senha" htmlFor="password" error={errors.password?.message} hint="Mínimo de 8 caracteres">
            <Input id="password" type="password" autoComplete="new-password" {...register('password')} />
          </Field>
          <Field label="Confirme a senha" htmlFor="passwordConfirmation" error={errors.passwordConfirmation?.message}>
            <Input id="passwordConfirmation" type="password" autoComplete="new-password" {...register('passwordConfirmation')} />
          </Field>
          <div className="sm:col-span-2">
            <Checkbox label="Autorizo ser cobrado em métodos alternativos em meus cartões múltiplos"
                      {...register('isAuthorizedFallback')} />
          </div>
        </div>
      </Card>

      <Card title="Documento">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Tipo" htmlFor="documentType">
            <Select id="documentType" {...register('documentType')}>
              {documentTypes.data.map((type) => (
                <option key={type.code} value={type.code}>{type.code === 'UE' ? 'UE (União Europeia)' : type.description}</option>
              ))}
            </Select>
          </Field>
          <Field label="País" htmlFor="documentCountry" error={errors.documentCountry?.message}>
            {selectedType?.countrySelectable ? (
              <Select id="documentCountry" {...register('documentCountry')}>
                {documentCountryOptions.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
              </Select>
            ) : (
              // CPF/SSN: país fixo (BR/US), sem abrir seleção.
              <Input id="documentCountry" readOnly className={lockedStyle} value={countryName(watch('documentCountry'))} />
            )}
          </Field>
          <Field label={isCpf ? 'CPF (somente números)' : 'Número do documento'} htmlFor="documentValue"
                 error={errors.documentValue?.message}>
            <Controller
              name="documentValue"
              control={control}
              render={({ field }) => (
                <Input
                  id="documentValue"
                  inputMode={isCpf ? 'numeric' : 'text'}
                  // Sem maxLength no CPF: quem COLA "529.982.247-25" (14 caracteres) teria o
                  // texto cortado pelo navegador ANTES do filtro de dígitos. O limite de 11
                  // dígitos é aplicado pelo onlyDigits abaixo.
                  maxLength={isCpf ? undefined : 40}
                  {...field}
                  // Regra 1.1.2.1: CPF não aceita nada além de números - filtrado na digitação.
                  onChange={(e) => field.onChange(isCpf ? onlyDigits(e.target.value, 11) : e.target.value)}
                />
              )}
            />
          </Field>
          <Field label="Descrição (opcional)" htmlFor="documentDescription">
            <Input id="documentDescription" {...register('documentDescription')} />
          </Field>
        </div>
      </Card>

      <Card title="Endereço">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Tipo" htmlFor="addressType">
            <Select id="addressType" {...register('addressType')}>
              {ADDRESS_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </Select>
          </Field>
          <Field label="Descrição" htmlFor="addressDescription" error={errors.addressDescription?.message}>
            <Input id="addressDescription" placeholder="Ex: Casa, Trabalho" {...register('addressDescription')} />
          </Field>

          <div className="flex flex-wrap gap-6 sm:col-span-2">
            <Checkbox label="Endereço internacional?" {...register('international')} />
            {/* Regra 1.1.3.2.2: em endereço internacional, "Sei meu CEP" fica desabilitado. */}
            <Checkbox label="Sei meu CEP" disabled={international} {...register('knowsZipCode')} />
          </div>

          <Field
            label={international ? 'Código postal' : 'CEP'}
            htmlFor="zipCode"
            error={errors.zipCode?.message}
            hint={!international && !knowsZipCode ? (
              <a className="underline" href="https://buscacepinter.correios.com.br/" target="_blank" rel="noreferrer">
                Não sabe seu CEP? Consulte nos Correios
              </a>
            ) : zipStatus === 'loading' ? 'Buscando endereço...' : undefined}
          >
            <Controller
              name="zipCode"
              control={control}
              render={({ field }) => (
                <Input
                  id="zipCode"
                  inputMode={international ? 'text' : 'numeric'}
                  // Mesmo motivo do CPF: "24220-000" colado tem 9 caracteres.
                  maxLength={international ? 20 : undefined}
                  {...field}
                  onChange={(e) => field.onChange(international ? e.target.value : onlyDigits(e.target.value, 8))}
                />
              )}
            />
          </Field>
          <Field label="País" htmlFor="addressCountry" error={errors.addressCountry?.message}>
            {international ? (
              <Select id="addressCountry" {...register('addressCountry')}>
                {countries.data.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
              </Select>
            ) : (
              <Input id="addressCountry" readOnly className={lockedStyle} value={countryName('BR')} />
            )}
          </Field>

          {zipMessage && <div className="sm:col-span-2"><Alert tone="info">{zipMessage}</Alert></div>}

          <div className="sm:col-span-2">
            <Field label="Endereço" htmlFor="addressName" error={errors.addressName?.message}>
              <Input id="addressName" readOnly={isLocked('addressName')}
                     className={isLocked('addressName') ? lockedStyle : ''} {...register('addressName')} />
            </Field>
          </div>
          <Field label="Número (opcional)" htmlFor="number" error={errors.number?.message}>
            <Controller
              name="number"
              control={control}
              render={({ field }) => (
                <Input id="number" inputMode="numeric" maxLength={10} {...field}
                       onChange={(e) => field.onChange(onlyDigits(e.target.value, 10))} />
              )}
            />
          </Field>
          <Field label="Complemento (opcional)" htmlFor="complement">
            <Input id="complement" {...register('complement')} />
          </Field>
          <Field label="Bairro" htmlFor="district">
            <Input id="district" readOnly={isLocked('district')}
                   className={isLocked('district') ? lockedStyle : ''} {...register('district')} />
          </Field>
          <Field label="Cidade" htmlFor="city">
            <Input id="city" readOnly={isLocked('city')} className={isLocked('city') ? lockedStyle : ''} {...register('city')} />
          </Field>
          <Field label="Estado / UF" htmlFor="state">
            <Input id="state" readOnly={isLocked('state')} className={isLocked('state') ? lockedStyle : ''} {...register('state')} />
          </Field>
        </div>
      </Card>

      <Card title="Telefone">
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="País" htmlFor="phoneCountry">
            <Select id="phoneCountry" {...register('phoneCountry')}>
              {countries.data.map((c) => <option key={c.code} value={c.code}>{c.name} ({c.dialCode})</option>)}
            </Select>
          </Field>
          <div className="sm:col-span-2">
            <Field label="Telefone (somente números)" htmlFor="phoneNumber" error={errors.phoneNumber?.message}>
              <Controller
                name="phoneNumber"
                control={control}
                render={({ field }) => (
                  <Input id="phoneNumber" inputMode="tel" maxLength={20} placeholder="21999999999" {...field}
                         onChange={(e) => field.onChange(onlyDigits(e.target.value, 20))} />
                )}
              />
            </Field>
          </div>
        </div>
      </Card>

      {registerMutation.isError && <Alert>{errorMessage(registerMutation.error)}</Alert>}
      <div className="flex items-center justify-between">
        <Link to="/login" className="text-sm text-slate-600 underline">Já tenho cadastro</Link>
        <Button type="submit" disabled={registerMutation.isPending}>
          {registerMutation.isPending ? 'Cadastrando...' : 'Cadastrar'}
        </Button>
      </div>
    </form>
  );
}

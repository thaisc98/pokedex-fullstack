import type { Metadata } from "next";
import { Suspense } from "react";
import { RegisterForm } from "@/features/auth/RegisterForm";

export const metadata: Metadata = { title: "Register" };

export default function RegisterPage() {
  return (
    <Suspense>
      <RegisterForm />
    </Suspense>
  );
}

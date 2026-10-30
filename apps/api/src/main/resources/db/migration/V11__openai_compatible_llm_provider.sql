-- OpenAI, Groq, Ollama, and OpenRouter became the single OPENAI_COMPATIBLE provider.
update ai_agent_configs
set llm_provider = 'OPENAI_COMPATIBLE'
where llm_provider in ('OPENAI', 'GROQ', 'OLLAMA', 'OPENROUTER');

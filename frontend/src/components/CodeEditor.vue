<template>
  <div ref="host" class="editor-host" :style="{ height }" />
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as monaco from 'monaco-editor'

const props = defineProps({
  modelValue: { type: String, default: '' },
  language: { type: String, default: 'PYTHON3' },
  height: { type: String, default: '100%' },
  readOnly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const LANG_MAP = {
  PYTHON3: 'python',
  CPP17: 'cpp',
  JAVA17: 'java'
}

const host = ref(null)
let editor = null

const THEME = {
  base: 'vs-dark',
  inherit: true,
  rules: [
    { token: 'comment', foreground: '5b6878', fontStyle: 'italic' },
    { token: 'keyword', foreground: 'ff7b72' },
    { token: 'string', foreground: 'a5d6ff' },
    { token: 'number', foreground: '79c0ff' },
    { token: 'type', foreground: 'ffa657' },
    { token: 'function', foreground: 'd2a8ff' },
    { token: 'variable', foreground: 'e6edf3' }
  ],
  colors: {
    'editor.background': '#0d1218',
    'editor.foreground': '#e6edf3',
    'editor.lineHighlightBackground': '#131a22',
    'editorLineNumber.foreground': '#39465a',
    'editorLineNumber.activeForeground': '#8b98a8',
    'editorCursor.foreground': '#2dd4bf',
    'editor.selectionBackground': '#1d3b3b',
    'editor.inactiveSelectionBackground': '#162a2a',
    'editorIndentGuide.background1': '#1a232e',
    'editorIndentGuide.activeBackground1': '#2a3543',
    'editorWidget.border': '#1d2731',
    'scrollbarSlider.background': '#26313d55',
    'scrollbarSlider.hoverBackground': '#33414f88'
  }
}

function resolveTheme() {
  return document.documentElement.dataset.theme === 'light' ? 'vs' : 'oj-dark'
}

function handleThemeChange(event) {
  if (editor) {
    monaco.editor.setTheme(event.detail === 'light' ? 'vs' : 'oj-dark')
  }
}

onMounted(() => {
  monaco.editor.defineTheme('oj-dark', THEME)
  editor = monaco.editor.create(host.value, {
    value: props.modelValue,
    language: LANG_MAP[props.language] || 'plaintext',
    theme: resolveTheme(),
    readOnly: props.readOnly,
    fontSize: 13.5,
    fontFamily: '"JetBrains Mono", "Cascadia Code", Consolas, monospace',
    fontLigatures: true,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    automaticLayout: true,
    padding: { top: 16, bottom: 16 },
    renderLineHighlight: 'all',
    smoothScrolling: true,
    cursorBlinking: 'smooth',
    cursorSmoothCaretAnimation: 'on',
    roundedSelection: false,
    tabSize: 4,
    lineNumbersMinChars: 3
  })
  editor.onDidChangeModelContent(() => {
    emit('update:modelValue', editor.getValue())
  })
  window.addEventListener('oj-theme-change', handleThemeChange)
})

watch(
  () => props.modelValue,
  (value) => {
    if (editor && value !== editor.getValue()) {
      editor.setValue(value || '')
    }
  }
)

watch(
  () => props.language,
  (value) => {
    if (editor) {
      monaco.editor.setModelLanguage(editor.getModel(), LANG_MAP[value] || 'plaintext')
    }
  }
)

onBeforeUnmount(() => {
  window.removeEventListener('oj-theme-change', handleThemeChange)
  editor?.dispose()
  editor = null
})
</script>

<style scoped>
.editor-host {
  width: 100%;
  min-height: 200px;
  overflow: hidden;
}
</style>
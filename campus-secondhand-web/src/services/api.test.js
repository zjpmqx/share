import { describe, expect, it } from 'vitest'
import {
  formatCategoryLabel,
  formatConditionLabel,
  normalizeCategoryValue,
  normalizeConditionValue,
} from './api'

describe('api enums normalization', () => {
  it('normalizes category aliases and casing', () => {
    expect(normalizeCategoryValue('fashion')).toBe('CLOTHING')
    expect(normalizeCategoryValue(' clothing ')).toBe('CLOTHING')
    expect(normalizeCategoryValue('')).toBe('')
    expect(normalizeCategoryValue(null)).toBe('')
  })

  it('normalizes condition numeric aliases', () => {
    expect(normalizeConditionValue('9')).toBe('LIKE_NEW')
    expect(normalizeConditionValue('8')).toBe('GOOD')
    expect(normalizeConditionValue('7')).toBe('FAIR')
    expect(normalizeConditionValue('6')).toBe('FAIR')
    expect(normalizeConditionValue(' new ')).toBe('NEW')
    expect(normalizeConditionValue(undefined)).toBe('')
  })

  it('formats normalized labels correctly', () => {
    expect(formatCategoryLabel('fashion')).toBe('服装配饰')
    expect(formatConditionLabel('9')).toBe('几乎全新')
    expect(formatCategoryLabel('UNKNOWN')).toBe('UNKNOWN')
    expect(formatConditionLabel('')).toBe('未知')
  })
})

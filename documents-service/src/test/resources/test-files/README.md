# Test Files Directory

This directory contains test files used by the DocumentProcessingPipeline tests.

## Files Needed

The following files are referenced by tests but need to be created manually or by the implementation:

### Required Test Files:

1. **test-document.pdf** - A simple PDF file for testing PDF processing
   - Should contain extractable text
   - Recommended: A simple document with a clear title
   - Size: < 1MB for fast tests

2. **test-document2.pdf** - A second PDF file for testing multiple files
   - Similar to test-document.pdf but different content
   - Size: < 1MB

3. **test-document.docx** - A DOCX file for testing Word document processing
   - Should contain extractable text
   - Recommended: A simple document with a clear title
   - Size: < 1MB

## Files Already Created:

- **test-document.txt** - Simple text file (already created)

## How to Create Test Files:

### Option 1: Manual Creation
1. Create simple documents using Word/LibreOffice
2. Add minimal content with a clear title
3. Save in the appropriate format
4. Place in this directory

### Option 2: Programmatic Creation (for CI/CD)
Consider creating test files programmatically in test setup if needed for automated testing.

## Note:

These test files should be committed to version control to ensure consistent test execution across environments.

For now, **test-document.txt** is sufficient to start TDD. The PDF and DOCX tests can be skipped or ignored until real files are created.
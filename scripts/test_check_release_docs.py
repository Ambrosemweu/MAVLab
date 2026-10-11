import unittest
import importlib.util
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("release_docs", Path(__file__).with_name("check-release-docs.py"))
release_docs = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release_docs)
targets, local_target, check = release_docs.targets, release_docs.local_target, release_docs.check


class DocumentationChecks(unittest.TestCase):
    def test_nested_video_poster_has_both_targets(self):
        self.assertEqual(targets("[![Demo](poster.jpg)](demo.mp4)"), ["poster.jpg", "demo.mp4"])

    def test_spaces_and_titles(self):
        self.assertEqual(targets('[File](<a b.md>) [Other](file.md "title")'), ["a b.md", "file.md"])

    def test_html_media(self):
        self.assertEqual(targets('<img src="screen.jpg"><a href="guide.md">Guide</a>'), ["screen.jpg", "guide.md"])

    def test_fenced_examples_are_not_links(self):
        self.assertEqual(targets("```md\n[example](missing.md)\n```\n[real](guide.md)"), ["guide.md"])

    def test_remote_and_fragment_links_are_skipped(self):
        for target in ("https://example.org/page", "mailto:test@example.org", "#heading"):
            self.assertIsNone(local_target(Path("/repo/docs/file.md"), target))

    def test_relative_uri_is_decoded(self):
        self.assertEqual(local_target(Path("/repo/docs/file.md"), "../a%20b.md#part"), Path("/repo/a b.md"))

    def test_publication_negative_control(self):
        original = Path.read_text

        def candidate_notes(path, *args, **kwargs):
            text = original(path, *args, **kwargs)
            if path.name.endswith("_release_notes.md"):
                text += "\nStatus: Release candidate\n"
            return text

        with patch.object(Path, "read_text", candidate_notes):
            *_, errors = check(publish=True)
        self.assertIn("Publication blocked: release notes are still candidate/draft", errors)

    def test_broken_link_negative_control(self):
        original = Path.read_text

        def broken_link(path, *args, **kwargs):
            text = original(path, *args, **kwargs)
            if path == release_docs.ROOT / "README.md":
                text += "\n[Missing](nonexistent-checker-test-file.md)\n"
            return text

        with patch.object(Path, "read_text", broken_link):
            *_, errors = check()
        self.assertTrue(any("unresolved local link nonexistent-checker-test-file.md" in error for error in errors))

    def test_wrong_tag_negative_control(self):
        *_, errors = check(tag="v0.0.0")
        self.assertTrue(any("does not match" in error for error in errors))


if __name__ == "__main__":
    unittest.main()

package uk.ac.ox.it;

import com.instructure.canvas.api.CoursesApi;

/**
 * Updates the course to use the new course image.
 */
public class CourseUpdater {

    private CoursesApi coursesApi;

    public CourseUpdater(CoursesApi coursesApi) {
        this.coursesApi = coursesApi;
    }

    public void updateImage(Integer courseId, Integer fileId) {
        coursesApi.updateCourse(courseId.toString(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, fileId, null, null, null, null, null, null);
    }
}
